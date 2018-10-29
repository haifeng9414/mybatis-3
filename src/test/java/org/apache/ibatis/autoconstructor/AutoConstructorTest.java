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
package org.apache.ibatis.autoconstructor;

import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.Reader;
import java.sql.Connection;
import java.util.*;

public class AutoConstructorTest {
    private static SqlSessionFactory sqlSessionFactory;

    @BeforeClass
    public static void setUp() throws Exception {
        // create a SqlSessionFactory
        final Reader reader = Resources.getResourceAsReader("org/apache/ibatis/autoconstructor/mybatis-config.xml");
        Properties properties = new Properties();
        properties.setProperty("demo", "a");
        properties.setProperty("org.apache.ibatis.parsing.PropertyParser.enable-default-value", "true");
        sqlSessionFactory = new SqlSessionFactoryBuilder().build(reader, properties);
        reader.close();

        // populate in-memory database
//        final SqlSession session = sqlSessionFactory.openSession();
//        final Connection conn = session.getConnection();
//        final Reader dbReader = Resources.getResourceAsReader("org/apache/ibatis/autoconstructor/CreateDB.sql");
//        final ScriptRunner runner = new ScriptRunner(conn);
//        runner.setLogWriter(null);
//        runner.runScript(dbReader);
//        conn.close();
//        dbReader.close();
//        session.close();
    }

    @Test
    public void fullyPopulatedSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            Map<String, String> map = new HashMap<>() {{
                put("name", "a");
            }};
            final PrimitiveSubject subject = mapper.getSubject(1, map, "name");
            mapper.getSubject(1, map, "name");
            Assert.assertNotNull(subject);
            System.out.println(subject.getName());
        } finally {
            sqlSession.close();
        }
    }

    @Test()
    public void primitiveSubjects() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            mapper.getSubjects();
        } finally {
            sqlSession.close();
        }
    }

    @Test()
    public void updateSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            int id = 1;
            String name = UUID.randomUUID().toString().substring(0, 5);
            System.out.println("update before:" + mapper.getSubject(id, new HashMap<>(), null));
            mapper.updateSubject(new PrimitiveSubject() {{
                setId(id);
                setName(name);
            }});
//            sqlSession.commit();
            System.out.println("update after:" + mapper.getSubject(id, new HashMap<>(), null));
        } finally {
            sqlSession.close();
        }
    }

    @Test()
    public void primitiveSubjectList() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            List<String> ids = new ArrayList<>();
            ids.add("1");
            ids.add("2");
            List<PrimitiveSubject> subjects = mapper.getSubjectList(ids);
            System.out.println(Arrays.toString(subjects.toArray()));
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void wrapperSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            verifySubjects(mapper.getWrapperSubjects());
        } finally {
            sqlSession.close();
        }
    }

    @Test
    public void annotatedSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            verifySubjects(mapper.getAnnotatedSubjects());
        } finally {
            sqlSession.close();
        }
    }

    @Test(expected = PersistenceException.class)
    public void badSubject() {
        final SqlSession sqlSession = sqlSessionFactory.openSession();
        try {
            final AutoConstructorMapper mapper = sqlSession.getMapper(AutoConstructorMapper.class);
            mapper.getBadSubjects();
        } finally {
            sqlSession.close();
        }
    }

    private void verifySubjects(final List<?> subjects) {
        Assert.assertNotNull(subjects);
        Assertions.assertThat(subjects.size()).isEqualTo(3);
    }
}
