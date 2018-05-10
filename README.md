## 阅读源码总结:

#### 简述执行一个查询语句的过程：

1. 首先入口是[SqlSessionFactoryBuilder][]，该类接受一个Reader来接收mybatis的配置文件，如[mybatis-config.xml][]，该文件的配置就不介绍了，
接收到配置文件后[SqlSessionFactoryBuilder][]使用[XMLConfigBuilder][]解析该配置文件并返回一个[Configuration][]对象，该对象保存了[mybatis-config.xml][]中的所有配置和[mybatis-config.xml][]文件中设置的Mapper文件的解析结果，
[Configuration][]的构造函数设置了所有mybatis中的默认别名、默认的TypeHandler及其他默认设置，该对象也将贯穿整个执行过程，解析Mapper的过程就是设置[Configuration的过程](#configuration_section)。

2. [XMLConfigBuilder][]解析完成后[SqlSessionFactoryBuilder][]创建一个[SqlSessionFactory][]接口的实现类[DefaultSqlSessionFactory][]，将解析到的[Configuration][]传入该对象并返回，
[SqlSessionFactory][]的功能是以不同的形式创建一个[SqlSession][]，下面是一个SqlSession的使用[demo][junit_demo]

        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            final PrimitiveSubject subject = mapper.getSubject(1);
            Assert.assertNotNull(subject);
        } finally {
            sqlSession.close();
        }

3. [SqlSession][]对象具有执行数据库的增删改查操作、获取Mapper和管理事务的功能，上面的代码中sqlSessionFactory获取[SqlSession][]对象的过程是：
    > 1. 获取[Configuration][]中保存的由[mybatis-config.xml][]中的environment元素解析而来的[Environment][]对象，该对象包含了连接数据库所需的相关信息并指定了[TransactionFactory][] (用于获取[Transaction][]，
    该类能够管理一个数据库连接的生命周期，包括创建、配置和commit/rollback)的实现类
    > 2. 获取[TransactionFactory][]并从中创建一个[Transaction][]，之后再创建一个[Executor][]，该对象代理了[Transaction][]的执行并且实现了对缓存的支持。
    > 3. 最后返回[DefaultSqlSession][]对象
    
4. 通过[SqlSession][]对象获取所需的Mapper，而Mapper保存在[Configuration][]对象的[MapperRegistry][]对象中，该对象维护了一个Mapper接口和该接口的[MapperProxyFactory][]的Map，
[MapperProxyFactory][]使用动态代理返回Mapper接口的代理对象[MapperProxy][]，对Mapper接口的方法调用都会交由[MapperProxy][]对象

5. [MapperProxy][]处理了可能存在的继承自Object类的方法调用及接口的default方法的调用后，创建一个[MapperMethod][]来执行Mapper方法的调用，
该对象根据当前数据库操作的类型调用[SqlSession][]对象的不同方法并对返回的数据进行必要的处理，如insert、delete、update影响的数据行。[SqlSession][]对象又是通过[Executor][]对象执行数据库操作，
[Executor][]对象创建一个[StatementHandler][]对象来获取和执行java.sql.Statement类，如果是查询操作则在执行完后通过[ResultSetHandler][]处理返回的结果，
将结果转换成Mapper接口的返回值类型并返回到调用mapper方法的地方。

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


#### <a name="configuration_section"></a>解析Configuration过程：
