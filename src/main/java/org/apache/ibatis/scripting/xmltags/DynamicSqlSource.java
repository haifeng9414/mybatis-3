/**
 * Copyright 2009-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ibatis.scripting.xmltags;

import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;

import java.util.Map;

/**
 * @author Clinton Begin
 */
public class DynamicSqlSource implements SqlSource {

    private final Configuration configuration;
    private final SqlNode rootSqlNode;

    public DynamicSqlSource(Configuration configuration, SqlNode rootSqlNode) {
        this.configuration = configuration;
        this.rootSqlNode = rootSqlNode;
    }

    /*
    parameterObject是传入mapper的参数，如
    PrimitiveSubject getSubject(@Param("id") final int id, @Param("param") Map<String, String> map, @Param("sort") String sort);
    方法将会产生6个参数：
    "param" -> 传入的map
    "id" -> "1"
    "sort" -> "name"
    "param3" -> "name"
    "param1" -> "1"
    "param2" -> 传入的map
     */
    @Override
    public BoundSql getBoundSql(Object parameterObject) {
        DynamicContext context = new DynamicContext(configuration, parameterObject);
        //解析动态SQL，解析完后将会移除所有动态SQL标签，只剩下#{}形式的变量
        rootSqlNode.apply(context);
        //MyBatis很多这种临时对象，很多实际上可以作为全局对象使用，但是使用临时对象可读性更好，代码更好看点
        SqlSourceBuilder sqlSourceParser = new SqlSourceBuilder(configuration);
        Class<?> parameterType = parameterObject == null ? Object.class : parameterObject.getClass();
        //sqlSourceParser.parse返回的是StaticSqlSource，sqlSourceParser解析时把所有的#{}变量用JDBC的变量占位符即?表示，
        //并把所有的#{}中指定的变量，如#{param.name}将会提取出param.name，保存到StaticSqlSource的parameterMappings属性中
        //ParameterMapping类包含了变量的名称即param.name、mode(默认是IN，即 id IN (select id from xxx)中的IN)，javaType等属性
        //ParameterMapping就是用来代表一个变量的
        SqlSource sqlSource = sqlSourceParser.parse(context.getSql(), parameterType, context.getBindings());
        //这里的sqlSource是StaticSqlSource类型的，StaticSqlSource的getBoundSql方法直接创建一个BoundSql对象并把自己的所有属性和
        //传入的parameterObject作为BoundSql的构造函数参数
        BoundSql boundSql = sqlSource.getBoundSql(parameterObject);
        for (Map.Entry<String, Object> entry : context.getBindings().entrySet()) {
            boundSql.setAdditionalParameter(entry.getKey(), entry.getValue());
        }
        return boundSql;
    }

}
