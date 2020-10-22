import java.util.*;
import java.util.concurrent.Semaphore;


public class Project2 extends Thread
{
	private final int  AllCustomers = 50;
	private final int AllWorkers = 3;
	private int WNum, CNum, PNum; //global variables to keep track 
	public Random r = new Random();
	
	public Semaphore max_customers = new Semaphore(10); //this simulates the maximum amount of customers that can be in the post office at one time
	public Semaphore workers  = new Semaphore(3); //there are 3 workers, they need to be free/ready to serve
	
	public Semaphore scale  = new Semaphore(1); //there is only 1 scale in the post office
	public Semaphore checkout_station  = new Semaphore(3); //there are three checkout stations
	
	public Semaphore mutex  = new Semaphore(1); //only let one worker serve one customer at a time
	
	public Semaphore checkout_ready = new Semaphore(0); //signal to the customer there is a checkout area ready
	public Semaphore customer_ready = new Semaphore(0); //signal to the worker there is a customer ready to be served
	public Semaphore enter_checkout = new Semaphore(0); //signal that the customer has entered the checkout area
	public Semaphore leave_checkout = new Semaphore(0); //signal that the customer has left the checkout area
	
	public Semaphore purchase_ready = new Semaphore(1); //signal that a purchase can be started
	public Semaphore start_purchase = new Semaphore(0); //let the customer start purchasing what they want
	public Semaphore purchase_completed = new Semaphore(0); //signal that the customer is finished purchasing
	public static Semaphore finished[] = new Semaphore[50]; //keep track of customers that have completed purchases
	
	public static void main(String[] args) 
	{
        Project2 p2 = new Project2(); //start post office simulation
        p2.PostOffice();
    }
	
	public void PostOffice()
	{
		//create threads and objects
		Thread[] c = new Thread[AllCustomers];
		Customer[] clist = new Customer[AllCustomers];
		Thread[] w = new Thread[AllWorkers];
		Worker[] wlist = new Worker[AllWorkers];
		
		for(int counter = 0; counter < AllCustomers; counter++)
		{
			finished[counter] = new Semaphore(0); //initialize finished semaphore array to 0
		}
		
		System.out.println("Simulating Post Office with 50 customers and 3 postal workers");
		System.out.println("");
		
		for(int counter = 0; counter < AllCustomers; counter++) //create and start customers
		{
			int PurchaseNr = r.nextInt(3) + 1;
			clist[counter] = new Customer(counter, PurchaseNr);
			System.out.println("Customer " + counter + " created");
			c[counter] = new Thread(clist[counter]);
			c[counter].start();
		}
		
		for(int counter = 0; counter < AllWorkers; counter++) //create and start workers
		{
			wlist[counter] = new Worker(counter);
			w[counter] = new Thread(wlist[counter]);
			System.out.println("Postal Worker " + counter + " created");
			w[counter].start();
		}
		
		for(int counter = 0; counter < AllCustomers; counter++) //join customer threads
		{
			try
			{
				c[counter].join();
				System.out.println("Joined customer "+ counter);
			}
			catch(InterruptedException e) {}
		}
		System.exit(0);
	}
	
	
	class Customer implements Runnable //customer class
	{
		//place keeping variables
		int CustNr;
		int PurchaseNr;
		int WorkerNr;
		
		private Customer(int CustNr, int PurchaseNr)
		{
			this.CustNr = CustNr;
			this.PurchaseNr = PurchaseNr;			
		}
		
		public void run()
		{
			try 
			{
				max_customers.acquire(); //wait for there to be room in the shop
				System.out.println("Customer " + CustNr + " enters post office"); 
				purchase_ready.acquire(); //wait to be allowed to start purchasing
				checkout_station.acquire(); //see if there are 3 customers already purchasing
			}
			catch(InterruptedException e) {}
			
			customer_ready.release(); //send signal that customer has been created and is ready to be served
			CNum = this.CustNr; //set global track-keeping variables
			PNum = this.PurchaseNr; //set global track-keeping variables
			
			enter_checkout.release(); //send signal that customer is in the checkout area
			
			try
			{
				checkout_ready.acquire(); //wait until there is a checkout station ready
			}
			catch(InterruptedException e) {}
			
			WorkerNr = WNum; //set track-keeping variable
			
			switch(this.PurchaseNr) //output for random purchase
			{
				case 1:
				{
					System.out.println("Customer " + CustNr + " asks postal worker " + this.WorkerNr + " to buy stamps");
					break;
				}
				case 2:
				{
					System.out.println("Customer " + CustNr + " asks postal worker "+ this.WorkerNr +" to mail a letter");
					break;
				}
				case 3:
				{
					System.out.println("Customer " + CustNr + " asks postal worker " +this.WorkerNr +" to mail a package");
					break;
				}
				default:
				{
					System.out.println("This should not happen.");
					break;
				}
				
			}
			
			start_purchase.release(); //signal to the worker to begin working on the order
			
			try
			{
				finished[CustNr].acquire(); //receive signal that customer has completed purchase
				purchase_completed.acquire(); //receive signal that the worker has completed the purchase
			}
			catch(InterruptedException e) {}
			
			checkout_station.release(); //send signal that a checkout station has opened up
			leave_checkout.release(); //send signal that customer has left checkout station
			max_customers.release(); //send signal that customer has left the post office
			System.out.println("Customer "+ CustNr + " leaves post office");
		}
	}
	
	class Worker implements Runnable //worker class
	{
		//track-keeping variables
		int WorkerNr;
		int CustNr;
		int PurchaseNr;
		
		private Worker(int WorkerNr)
		{
			this.WorkerNr = WorkerNr;
		}
		
		public void run()
		{
			while(true)
			{
				try
				{
					customer_ready.acquire(); //wait until the customer is ready to be served
					workers.acquire(); //wait until the workers are ready to serve
					enter_checkout.acquire(); //wait until a customer is in the checkout area
					mutex.acquire(); //make sure that only one worker serves one customer at a time
				}
				catch(InterruptedException e) {}
				
				
				this.CustNr = CNum; //set global track-keeping variables
				System.out.println("Postal Worker "+ WorkerNr +" serving Customer "+ this.CustNr);
				WNum = this.WorkerNr; //set global track-keeping variables
				
				purchase_ready.release(); //signal that the worker is ready to carry out a purchase
				checkout_ready.release(); //signal that a checkout station is ready 
				this.PurchaseNr = PNum;
				
				try
				{
					start_purchase.acquire(); //wait until the customer has started a purchase
				}
				catch(InterruptedException e) {}
				
				mutex.release(); //release the restriction since we don't need it anymore
				
				switch(this.PurchaseNr) //carryout purchase
				{
					case 1:
					{
						try
						{
							Thread.sleep(1000); 
						}
						catch(InterruptedException e) {}
						break;
					}
					case 2:
					{
						try
						{
							Thread.sleep(1500); 
						}
						catch(InterruptedException e) {}
						break;
					}
					case 3:
					{
						try
						{
							scale.acquire();
						}
						catch(InterruptedException e) {}
						
						System.out.println("Scales in use by postal worker " + WorkerNr);
						
						try
						{
							Thread.sleep(2000); 
						}
						catch(InterruptedException e) {}
						
						System.out.println("Scales released by postal worker "+ WorkerNr);
						
						scale.release();
						break;
					}
					default:
					{
						System.out.println("This should not happen.");
						break;
					}
				}
				
				System.out.println("Postal worker " + WorkerNr + " finished serving customer " +this.CustNr);
				finished[CustNr].release(); //signal that the customer has been served
				
				switch(this.PurchaseNr) //output
				{
					case 1:
					{
						System.out.println("Customer " + this.CustNr + " finished buying stamps");
						break;
					}
					case 2:
					{
						System.out.println("Customer " + this.CustNr + " finished mailing a letter");
						break;
					}
					case 3:
					{
						System.out.println("Customer " + this.CustNr + " finished mailing a package");
						break;
					}
					default:
					{
						System.out.println("This should not happen.");
						break;
					}
				}
				
				purchase_completed.release(); //signal that the purchase is completed
				
				try
				{
					leave_checkout.acquire(); //wait until the customer has left the checkout station
				}
				catch(InterruptedException e) {}
				
				workers.release(); //signal that the worker is free				
			}	
		}	
	}
}
