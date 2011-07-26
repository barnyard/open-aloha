/*
 * Aloha Open Source SIP Application Server- https://trac.osmosoft.com/Aloha
 *
 * Copyright (c) 2008, British Telecommunications plc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation; either version 3.0 of the License, or (at your option) any later
 * version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package com.bt.sdk.rnd.spike;

import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Driver {

	private Dao dao;

	public Driver(Dao dao){
		this.dao = dao;
	}

	public void initialize(){
		if(!dao.tableExists())
			dao.createTable();
		else
			dao.truncateTable();
	}

	public void sequence1(){
		int id = dao.nextId();
		dao.insert(id, "label1");
		String label1 = dao.get(id);
		//System.out.println("after insert - " + id + ": " + label1);
		id = dao.nextId();
		dao.insert(id, "label2");
		dao.update(id, "newLabel2");
		String label2 = dao.get(id);
		//System.out.println("after update - " + id + ": " + label2);
		dao.delete(id);
		try{
			dao.get(id);
		} catch(IllegalArgumentException e){
			//System.out.println(e.getMessage());
		}
	}

	public void sleep(long ms){
		try{
			Thread.sleep(ms);
		} catch (InterruptedException e){

		}
	}

	public static void main(String[] args) throws Exception{
		try{
			ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext("applicationContext.xml");
			Dao dao = (Dao)ctx.getBean("daoBean");
			Driver driver = new Driver(dao);
			driver.initialize();
			int count = 0;
			//doDbStart(ctx)
			while(count++<100){
				driver.sequence1();
				driver.sleep(100);
				if(count % 10 == 0)
				{
					System.out.println(count + " ================================== ");
				}

				//doDbRestart(count, ctx);

			}
			System.out.println("Data size on database (ha)" + dao.getAll().size());

			Dao dao1 = (Dao)ctx.getBean("hsqldb1.daoBean");
			Dao dao0 = (Dao)ctx.getBean("hsqldb0.daoBean");
			System.out.println("Data size on database  (1): " + dao0.getAll().size());
			System.out.println("Data size on database  (2): " + dao1.getAll().size());
			System.out.println("The three above results should match!");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.exit(0);
		}
	}

	static void doDbStart(ClassPathXmlApplicationContext ctx){
		Hsqldb db0 = (Hsqldb)ctx.getBean("hsqldb0Bean");
		Hsqldb db1 = (Hsqldb)ctx.getBean("hsqldb1Bean");
		db0.start();
		db1.start();
	}

	static void doDbRestart(int count, ClassPathXmlApplicationContext ctx)
			throws InterruptedException {
		Hsqldb db1 = (Hsqldb)ctx.getBean("hsqldb1Bean");
		Dao dao1 = (Dao)ctx.getBean("hsqldb1.daoBean");
		if(count == 33)
		{
			System.out.println("Stopping db 1");
			db1.stop();
		}

		if(count== 66){
			System.out.println("Starting db 1");
			db1.start();
			Thread.sleep(1000);
			dao1.createTable();
		}
	}
}
