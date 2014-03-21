package org.hibernate.test.lazyload;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Oleksander Dukhno
 */
public class LazyLoadingTest
		extends BaseCoreFunctionalTestCase {

	private static final int CHILDREN_SIZE = 3;
	private Long parentID;
	private Long lastChildID;

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class
		};
	}

	protected void prepareTest()
			throws Exception {
		Session s = openSession();
		s.beginTransaction();

		List<Child> children = new ArrayList<Child>();
		for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
			Child c = new Child();
			s.persist( c );
			lastChildID = c.getId();
			children.add( c );
		}
		Parent p = new Parent();
		p.setChildren( children );
		s.persist( p );
		parentID = p.getId();

		s.getTransaction().commit();
		s.clear();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7971")
	public void testLazyCollectionLoadingAfterEndTransaction() {
		Session s = openSession();
		s.beginTransaction();
		Parent loadedPatent = (Parent) s.load( Parent.class, parentID );
		s.getTransaction().commit();
		s.close();

		int i = 0;
		for ( Child child : loadedPatent.getChildren() ) {
			i++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, i );

		s = openSession();
		s.beginTransaction();
		Child loadedChild = (Child) s.load( Child.class, lastChildID );
		s.getTransaction().commit();
		s.close();

		Parent p = loadedChild.getParent();
		int j = 0;
		for ( Child child : p.getChildren() ) {
			j++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, j );
	}

}