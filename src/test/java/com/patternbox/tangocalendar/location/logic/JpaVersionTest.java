/**************************** Copyright notice ********************************

Copyright (C)2014 by D. Ehms, http://www.patternbox.com
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.
 ******************************************************************************/
package com.patternbox.tangocalendar.location.logic;

import static org.junit.Assert.assertEquals;

import java.util.List;

import javax.persistence.EntityManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.patternbox.jee7.jpa.Order;
import com.patternbox.jee7.jpa.Product;

import de.akquinet.jbosscc.needle.db.transaction.VoidRunnable;
import de.akquinet.jbosscc.needle.junit.DatabaseRule;
import de.akquinet.jbosscc.needle.junit.NeedleRule;

/**
 * Unit test for tracking version management by JPA.
 * 
 * @author <a href='http://www.patternbox.com'>D. Ehms, Patternbox</a>
 */
public class JpaVersionTest {

	@Rule
	public DatabaseRule databaseRule = new DatabaseRule();

	@Rule
	public NeedleRule needleRule = new NeedleRule(databaseRule);

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	private void increaseProductVersion(final EntityManager em, final Long productId)
			throws Exception {
		Product product = em.find(Product.class, productId);
		Long version = product.getVersion();
		product.setName(product.getName() + "_X");
		em.merge(product);
		em.flush();
		product = em.find(Product.class, productId);
		assertEquals(version + 1, product.getVersion().longValue());
	}

	/**
	 * Test method.
	 */
	@Test
	public void testStoreLocation() throws Exception {
		// insert products into database
		databaseRule.getTransactionHelper().executeInTransaction(new VoidRunnable() {

			@Override
			public void doRun(EntityManager em) throws Exception {
				em.persist(new Product("Product-1", 1.23));
				em.persist(new Product("Product-2", 2.34));
				List<Product> products = em.createNamedQuery("All_Products", Product.class).getResultList();
				System.out.println(products);
			}
		});
		// assign product to an order line and check version stability of the product
		databaseRule.getTransactionHelper().executeInTransaction(new VoidRunnable() {

			@Override
			public void doRun(EntityManager em) throws Exception {
				Product product = em.find(Product.class, 1L);
				Order order = new Order("MyOrder-1");
				order.addProduct(product, 3);
				em.persist(order);
				List<Product> products = em.createNamedQuery("All_Products", Product.class).getResultList();
				System.out.println(products);
			}
		});
		// what happens with an order if the referenced product is updated in the meanwhile
		databaseRule.getTransactionHelper().executeInTransaction(new VoidRunnable() {

			@Override
			public void doRun(EntityManager em) throws Exception {
				Product product = em.find(Product.class, 1L);
				increaseProductVersion(em, product.getIdentifier());
				Order order = new Order("MyOrder-1");
				// em.detach(product);
				order.addProduct(product, 3);
				em.persist(order);
				List<Product> products = em.createNamedQuery("All_Products", Product.class).getResultList();
				System.out.println(products);
			}
		});
	}
}
