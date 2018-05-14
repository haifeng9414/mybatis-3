## 个人总结:

#### 简述执行一个查询语句的过程：

1. 首先入口是[SqlSessionFactoryBuilder][]，该类接受一个Reader来接收mybatis的配置文件，如[mybatis-config.xml][]，该文件的配置就不介绍了，
接收到配置文件后[SqlSessionFactoryBuilder][]使用[XMLConfigBuilder][]解析该配置文件并返回一个[Configuration][]对象，该对象保存了[mybatis-config.xml][]中的所有配置和[mybatis-config.xml][]文件中设置的Mapper文件的解析结果，
[Configuration][]的构造函数设置了所有mybatis中的默认别名、默认的TypeHandler及其他默认设置，该对象也将贯穿整个执行过程，解析Mapper的过程就是设置[Configuration的过程](#configuration_section)。

2. [XMLConfigBuilder][]解析完成后[SqlSessionFactoryBuilder][]创建一个[SqlSessionFactory][]接口的实现类[DefaultSqlSessionFactory][]，将解析到的[Configuration][]传入该对象并返回，
[SqlSessionFactory][]的功能是以不同的形式创建一个[SqlSession][]，下面是一个SqlSession的使用[demo][junit_demo]。
    ```java
    final SqlSession sqlSession = sqlSessionFactory.openSession();
    try {
        final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
        final PrimitiveSubject subject = mapper.getSubject(1);
        Assert.assertNotNull(subject);
    } finally {
        sqlSession.close();
    }
    ```

3. [SqlSession][]对象具有执行数据库的增删改查操作、获取Mapper和管理事务的功能，上面的代码中sqlSessionFactory获取[SqlSession][]对象的过程是：
    1. 获取[Configuration][]中保存的由[mybatis-config.xml][]中的environment元素解析而来的[Environment][]对象，该对象包含了连接数据库所需的相关信息并指定了[TransactionFactory][] (用于获取[Transaction][]，
    该类能够管理一个数据库连接的生命周期，包括创建、配置和commit/rollback)的实现类。
    1. 获取[TransactionFactory][]并从中创建一个[Transaction][]，之后再创建一个[Executor][]，该对象代理了[Transaction][]的执行并且实现了对缓存的支持。
    1. 最后返回[DefaultSqlSession][]对象。
    
4. 通过[SqlSession][]对象获取所需的Mapper，而Mapper保存在[Configuration][]对象的[MapperRegistry][]对象中，该对象维护了一个Mapper接口和该接口的[MapperProxyFactory][]的Map，
[MapperProxyFactory][]使用动态代理返回Mapper接口的代理对象[MapperProxy][]，对Mapper接口的方法调用都会交由[MapperProxy][]对象。

5. [MapperProxy][]处理了可能存在的继承自Object类的方法调用及接口的default方法的调用后，创建一个[MapperMethod][]来执行Mapper方法的调用，
该对象根据当前数据库操作的类型调用[SqlSession][]对象的不同方法并对返回的数据进行必要的处理，如insert、delete、update影响的数据行。[SqlSession][]对象又是通过[Executor][]对象执行数据库操作，
[Executor][]对象创建[StatementHandler][]对象来获取和执行java.sql.Statement类，如果是查询操作则在执行完后通过[ResultSetHandler][]处理返回的结果，
将结果转换成Mapper接口的返回值类型并返回到调用mapper方法的地方。

6. 从[SqlSession][]获取Mapper接口的实例到最终执行数据库操作涉及到了多个对象，每个对象都有自己的功能:
    1. [MapperProxy][]:执行对Object类上的方法和default方法的调用，其他的方法调用都是Mapper接口方法的调用，[MapperProxy][]创建[MapperMethod][]对象并将其他方法的调用交由该对象执行。
    1. [MapperMethod][]:将传入到当前调用的Mapper接口上的方法的参数转换成Map(当存在Param注解指定了参数名或者参数数量大于1时使用注解指定的名称或arg0、param0、arg1、param1...作为参数名并以参数名为key、值为value)或直接返回参数值(不存在Param注解并且只有一个参数时)，
    根据执行的方法对应的数据库操作(insert|delete|update|select)调用[SqlSession][]对象的对应方法，并在返回结果之前对结果进行处理如将结果添加到List中并返回。
    1. [SqlSession][]:获取保存在[Configuration][]中的当前的Mapper方法对应的[MappedStatement][] ([SQL语句的解析过程](#MappedStatement_section))，处理传入的参数，这里对参数的处理不同于[MapperMethod][]中的参数处理，
    [MapperMethod][]处理的主要目的是确定参数的名称，[MappedStatement][]中的处理是如果方法只有一个参数并且是集合或者数组时，根据类型为参数设置上collection、list或array等别名(个人认为这一步在[MapperMethod][]做也可以，
    [MapperMethod][]中使用的是[ParamNameResolver][]解析的参数名，从类名上看得出这是专门用于解析参数名的类，这和[MappedStatement][]中解析参数的功能类似，
    如果再创建一个类包含了[MappedStatement][]中解析参数功能，并且[ParamNameResolver][]作为其成员变量使其也拥有了确定参数名的功能，则这样的一个类放到[MapperMethod][]中取代现有的[ParamNameResolver][]的调用我觉得也是可以的，
    或者直接修改现有的[ParamNameResolver][]确定参数名称的过程也可以，不确定mybatis现在这种做法的好处是什么，当然现在这么做也没问题)，之后调用[Executor][]类执行数据库操作。
    1. [Executor][]:实现了缓存、事务的commit/rollback操作和数据库操作，执行数据库操作时创建[StatementHandler][]，使用该对象获取数据库连接和创建java.sql.Statement对象并交由[StatementHandler][]执行数据库操作
    1. [StatementHandler][]:最终执行数据库操作的类，在调用java.sql.Statement类的execute方法并获取到结果后对结果进行处理并返回

[SqlSessionFactoryBuilder]: src/main/java/org/apache/ibatis/session/SqlSessionFactoryBuilder.java
[mybatis-config.xml]: src/test/java/org/apache/ibatis/autoconstructor/mybatis-config.xml
[XMLConfigBuilder]: src/main/java/org/apache/ibatis/builder/xml/XMLConfigBuilder.java
[Configuration]: src/main/java/org/apache/ibatis/session/Configuration.java
[SqlSessionFactory]: src/main/java/org/apache/ibatis/session/SqlSessionFactory.java
[DefaultSqlSessionFactory]: src/main/java/org/apache/ibatis/session/defaults/DefaultSqlSessionFactory.java
[SqlSession]: src/main/java/org/apache/ibatis/session/SqlSession.java
[junit_demo]: src/test/java/org/apache/ibatis/autoconstructor/AutoConstructorTest.java
[Environment]: src/main/java/org/apache/ibatis/mapping/Environment.java
[TransactionFactory]: src/main/java/org/apache/ibatis/transaction/TransactionFactory.java
[Transaction]: src/main/java/org/apache/ibatis/transaction/Transaction.java
[Executor]: src/main/java/org/apache/ibatis/executor/Executor.java
[DefaultSqlSession]: src/main/java/org/apache/ibatis/session/defaults/DefaultSqlSession.java
[MapperRegistry]: src/main/java/org/apache/ibatis/binding/MapperRegistry.java
[MapperProxyFactory]: src/main/java/org/apache/ibatis/binding/MapperProxyFactory.java
[MapperProxy]: src/main/java/org/apache/ibatis/binding/MapperProxy.java
[MapperMethod]: src/main/java/org/apache/ibatis/binding/MapperMethod.java
[StatementHandler]: src/main/java/org/apache/ibatis/executor/statement/StatementHandler.java
[ResultSetHandler]: src/main/java/org/apache/ibatis/executor/resultset/ResultSetHandler.java
[MappedStatement]: src/main/java/org/apache/ibatis/mapping/MappedStatement.java 
[ParamNameResolver]: src/main/java/org/apache/ibatis/reflection/ParamNameResolver.java

#### <a name="configuration_section"></a>解析Configuration过程：

1. 下面是解析Configuration的顺序:
```java
propertiesElement(root.evalNode("properties"));
Properties settings = settingsAsProperties(root.evalNode("settings"));
loadCustomVfs(settings);
typeAliasesElement(root.evalNode("typeAliases"));
pluginElement(root.evalNode("plugins"));
objectFactoryElement(root.evalNode("objectFactory"));
objectWrapperFactoryElement(root.evalNode("objectWrapperFactory"));
reflectorFactoryElement(root.evalNode("reflectorFactory"));
settingsElement(settings);
environmentsElement(root.evalNode("environments"));
databaseIdProviderElement(root.evalNode("databaseIdProvider"));
typeHandlerElement(root.evalNode("typeHandlers"));
mapperElement(root.evalNode("mappers")); 
```
利用XPath解析XML配置文件，`propertiesElement`方法解析XML中的properties节点，在创建对应的XNode时使用[PropertyParser][]解析${value}和${value:defaultValue}形式的属性值。
`loadCustomVfs`使用ClassLoader加载VFS，VFS用于加载指定路径下的jar文件或class文件。`settingsElement`设置mybatis中的各项参数如是否开启缓存，最主要的是`mapperElement`方法，
该方法解析mapper文件或mapper接口，`mapperElement`方法分两种情况，一种解析接口，一种解析XML：

1. 接口解析:调用[Configuration][]的`addMapper`或`addMappers`方法添加单个接口或包路径下接口，[Configuration][]利用[MapperRegistry][]添加Mapper接口，
[MapperRegistry][]创建当前解析的接口的[MapperProxyFactory][]对象并保存下来，之后创建[MapperAnnotationBuilder][]对象解析当前接口，[MapperAnnotationBuilder][]首先判断当前接口包路径下是否存在同名的Mapper的XML文件，
如果存在则使用[XMLMapperBuilder][]解析该XML，解析过程中涉及到的尚未解析到的引用将会被添加到[Configuration][]的incompleteXXX中，这些incompleteXXX是所有解析Mapper接口和XML文件共享的，
每个解析过程都会获取一次所有的incompleteXXX并尝试再次解析，所以能保证当前尚未解析到的incompleteXXX在完成所有的Mapper接口和XML文件解析过程之前肯定能完成解析。解析时以接口类的toString结果作为当前接口资源的namespace，
如`interface org.apache.ibatis.autoconstructor.AutoConstructorMapper`，之后获取类上的[CacheNamespace][]注解和[CacheNamespaceRef][]注解，[CacheNamespace][]配置了指定namespace的缓存属性，
[CacheNamespaceRef][]指定了引用哪个namespace的缓存。之后解析每个接口方法的注解方法上可用的注解有`Insert、Update、Delete、Select、InsertProvider、UpdateProvider、DeleteProvider、SelectProvider、Options、SelectKey、ConstructorArgs、TypeDiscriminator、Results、MapKey、ResultMap、ResultType、Flush`
    1. Insert、Update、Delete、Select、InsertProvider、UpdateProvider、DeleteProvider、SelectProvider:指定当前方法的SQL语句，XXXProvider能够指定类名和返回在运行时执行的 SQL 语句的方法。
    1. Options:用于设置映射语句的属性如`useCache`，由于Java的注解不能设置值为null，所以`Options`包含了很多默认值，使用时需要注意。
    1. SelectKey:用于设置生成某个属性值的SQL语句，如`@SelectKey(statement = "call identity()", keyProperty = "nameId", before = false, resultType = int.class)`，解析SelectKey时以当前方法的id + "!selectKey"为SelectKey的id，这样就能将SelectKey和方法进行绑定，之后将SelectKey中的SQL解析成[MappedStatement][]添加到[Configuration][]中，并根据该[MappedStatement][]创建[SelectKeyGenerator][]添加到[Configuration][]，
    [SelectKeyGenerator][]实现了[KeyGenerator][]接口，用于在运行SQL时对SQL进行处理。
    1. ResultMap:为`Select`或`SelectProvider`注解设置在XML中配置的`ResultMap`的id
    1. ConstructorArgs:设置返回结果的构造函数，如
    ```java
    @ConstructorArgs({
        @Arg(property = "id", column = "cid", id = true),
        @Arg(property = "name", column = "name")
    })
    ```
    
2. XML解析:直接使用[XMLMapperBuilder][]解析，解析过程和使用[MapperAnnotationBuilder][]解析过程类似，只不过配置都写在了XML中，XML中的namespace就是该XML所配置的Mapper接口的全路径，
在XML解析完成会以namespace作为class名添加到[Configuration][]中。

[PropertyParser]: src/main/java/org/apache/ibatis/parsing/PropertyParser.java
[Configuration]: src/main/java/org/apache/ibatis/session/Configuration.java
[MapperRegistry]: src/main/java/org/apache/ibatis/binding/MapperRegistry.java
[MapperProxyFactory]: src/main/java/org/apache/ibatis/binding/MapperProxyFactory.java
[MapperAnnotationBuilder]: src/main/java/org/apache/ibatis/builder/annotation/MapperAnnotationBuilder.java
[XMLMapperBuilder]: src/main/java/org/apache/ibatis/builder/xml/XMLMapperBuilder.java
[CacheNamespace]: src/main/java/org/apache/ibatis/annotations/CacheNamespace.java
[CacheNamespaceRef]: src/main/java/org/apache/ibatis/annotations/CacheNamespaceRef.java
[SelectKeyGenerator]: src/main/java/org/apache/ibatis/executor/keygen/SelectKeyGenerator.java
[KeyGenerator]: src/main/java/org/apache/ibatis/executor/keygen/KeyGenerator.java

#### <a name="MappedStatement_section"></a>SQL语句的解析过程，如何支持变量和动态SQL：

`todo`

#### 如何实现一级缓存和二级缓存

`todo`

#### 如何实现拦截器

`todo`

#### SelectKey

`todo`

#### 待补充...
