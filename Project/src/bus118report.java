

/*************************************************************
PROGRAM TO MAXIMIZE POWER COMMITMENT FROM SOLAR LOCATIONS 
USES IEEE 118 BUS SYSTEM
**************************************************************/

/*Header files section*/

import java.io.*;
import com.opencsv.*;
import ilog.concert.*;
import ilog.cplex.*;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.commons.math3.complex.*;
import Jama.*;

import java.util.LinkedList;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.complex.Complex;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

public class bus118report {

	/* Static variable declarations */
	
	static int noOfDays = 15, noOfLocations = 25, noOfSlots = 48, noOfBuses = 118, noOfBranches = 186, loadBusCount = 64, genBusCount = 54, selected = 6, colcount = 25, vertices = 120;
	
	// There are 12 time intervals in a slot of 3 hours i.e, 15 minutes interval duration
	
	static int interval = 1;  
	static double bmva = 100.0, thetaMin = -.785398, thetaMax = .785398;
	
	// Total demand in a basic IEEE 14 bus system without solar power injection is 4242 MW
	
	static double  demand = 4242;
	
	// Declare path for grid data file
	
	static String gridPath = "C:\\Users\\Arghya\\Documents\\BTP\\Project_Anoop\\Project_Anoop\\input files\\bus data\\loadflowdata118.xls";
	
	/* Main method */
	
	public static void main(String[] args) {
		
		// csvFilePrefix stores path of history solar data
		
		String csvFilePrefix = "C:\\Users\\Arghya\\Documents\\BTP\\Project_Anoop\\Project_Anoop\\input files\\solar data\\";
		
		// Declare variables to store actual weather data corresponding to different time slots
		// Here we consider 4 time slots from 6:00 am to 6:00 pm, each of 3 hours duration
		
		double[][][] Act = new double [noOfSlots][noOfDays][noOfLocations];

		
		// Declare variables to store predicted weather data corresponding to different time slots
		
		double[][][] Pre = new double [noOfSlots][noOfDays][noOfLocations];
		
		String line = "";
        String cvsSplitBy = "	";
        int[] Index = new int [noOfSlots];

        for (int i=0;i<noOfSlots;i++)
        {
        	Index[i]=0;
        }


        for(int i = 1;i <= noOfDays;i++)
        {
        	//System.out.println("input file\t" + i);
        	try{
        		
        		// Declare count to keep track of line number
        		
            	int count = 0;
            	
            	// Create file name based on date
            	
            	String fileName = csvFilePrefix + i + "March.csv";  
            	
            	// Declare stream reader for input from file
            	
            	BufferedReader br = new BufferedReader(new FileReader(fileName));
            	
            	// Read till last line
            	
            	while((line = br.readLine()) != null){
            		
            		// Reject data corresponding to 00:00 am to 6:00 am
            		
            		if(count < 25){
            			String[] values = line.split(cvsSplitBy);
            			if(values.length == 3)            				
            				count++;
            		}
            		
            		// Reject data corresponding to 6:00 pm to 00:00 am
            		
            		else if(count > 72){
            			String[] values = line.split(cvsSplitBy);
            			if(values.length == 3)            				
            				count++;
            		}
            		
            		// Process data corresponding to 6:00 am to 6:00 pm
            		
            		else{
    	        		String[] values = line.split(cvsSplitBy);
    	        		
    	        		// If valid line
    	        		
    	        		if(values.length == 3){
    	        			
    	        			// Convert cell contents into numeric values
    	        			
    	        			String value = values[1].substring(3, values[1].length() - 3);    	        		
    	        			String[] parts = value.split("\0");    	        			
    	        			StringBuilder newVal = new StringBuilder();
    	        			for(int j = 0;j < parts.length;j++)    	        				
    	        				newVal.append(parts[j]);
    	        			String finalVal = newVal.toString();
    	        			Double power = Double.parseDouble(finalVal);    	       

    	        			Act[count-25][Index[count-25]++][0] = power; 	
    	        			count++;

    	        		}
            		}
            	}
            }
        	
        	// Handle exceptions arise during execution
        	
            catch(IOException e){
            	System.out.println("erRor" + e);
            }
        }
        
        double change;
        
        // Generate actual data randomly for all locations using existing actual values
        
        for(int i = 1;i < noOfLocations;i++){
        	for(int j = 0;j < noOfDays * interval;j++){
        		
        		// Variable 'change' stores the random change made from the actual data
        		// ThreadLocalRandom.current().nextDouble(a, b) generate random double value with in range a to b
        		// Generate random change corresponding to all time slots
        		
        		//6am-8am & 4pm-6pm
        		for (int k=0; k<8; k++)
        		{
        			change = ThreadLocalRandom.current().nextDouble(5,30); 
        			Act[k][j][i] = Act[k][j][0] + change;
        			Act[noOfSlots-k-1][j][i] = Act[noOfSlots-k-1][j][0] + change;
        		}
        		
        		//8am-10am & 2pm-4pm
        		for (int k=8; k<16; k++)
        		{
        			change = ThreadLocalRandom.current().nextDouble(20, 50); 
        			Act[k][j][i] = Act[k][j][0] + change;
        			Act[noOfSlots-k-1][j][i] = Act[noOfSlots-k-1][j][0] + change;
        		}
        		
        		//10am-2pm (max solar insolation)
        		for (int k=16; k<24; k++)
        		{
        			change = ThreadLocalRandom.current().nextDouble(30 , 75); 
        			Act[k][j][i] = Act[k][j][0] + change;
        			Act[noOfSlots-k-1][j][i] = Act[noOfSlots-k-1][j][0] + change;
        		}
        		

        	}
        }
        
        // Generate forecast data randomly for all locations from corresponding actual data
        
        
        FileWriter fwrite, sdwrite;
		
		try {
			fwrite = new FileWriter("ActCommit.txt");
			PrintWriter p = new PrintWriter(fwrite);
			
			sdwrite = new FileWriter("SD.txt");
			PrintWriter sdw = new PrintWriter(sdwrite);
		
        
	        for(int i = 0;i < noOfDays ;i++){
	        	
	        	//p.println("\nDay "+ i);
	        	for(int j = 0;j < noOfLocations;j++){
	        		
	        		//p.println("\n\t Location "+ j);
	        		for (int k=0; k<noOfSlots; k++){
	
		        		// LowPer stores lower bound for random value range
		        		// minimum lower bound is zero
		        		
		        		//System.out.println("\t\t" +Act[k][i][j]+ "\t\t" );
		        		
		        		double LowPer = (Act[k][i][j] != 0) ? Act[k][i][j] - Act[k][i][j] * .5 : 0;
		        		
		        		// UpPer stores upper bound for random value range
		        		// minimum upper bound is 5 MW
		        		
		        		double UpPer = (Act[k][i][j] != 0) ? Act[k][i][j] + Act[k][i][j] * .5 : 5;
		        		
		        		// Generate forecast value from lower bound and upper bound
		        		
		        		Pre[k][i][j] = ThreadLocalRandom.current().nextDouble(LowPer, UpPer);
		        		

	
	
	        		}
        		

	        	}
        	}
		
	    
        
        // Declare variables to store forecast error for all time slots
        
        double[][][] Error = new double[noOfSlots][noOfDays][noOfLocations];

        
        // calculate forecast error by subtracting forecasted value from actual value
        
        for(int i = 0;i < noOfDays;i++){
        	for(int j = 0;j < noOfLocations;j++){
        		for (int k=0; k<noOfSlots; k++)
        		{
        			Error[k][i][j] = Act[k][i][j] - Pre[k][i][j];
        		}

        	}
        }
        
        // Declare variable to store mean forecast error values
        
        double[][] mean = new double[noOfSlots][noOfLocations];
        
        // Initialize mean as zero
        
        for(int i = 0;i < noOfSlots;i++)
        	for(int j = 0;j < noOfLocations;j++)
        		mean[i][j]=0.0;
       

       
        
        // Sum error values 
        
        for (int k=0; k<noOfSlots; k++){
        	for(int j = 0;j < noOfLocations;j++){
        		for(int i = 0;i < noOfDays ;i++)
        		{
        			mean[k][j] += Error[k][i][j];
        		}

        	}
        }
        
        // Calculate mean by dividing sum by no of data
        
        for(int i = 0;i < noOfSlots;i++){
        	for(int j = 0;j < noOfLocations;j++){
        		mean[i][j] = mean[i][j] / (noOfDays);
        	}
        }
        
        // Find forecast error standard deviation for all slots and locations
        // Declare a variable to store standard deviation
        
        double[][] sd = new double[noOfSlots][noOfLocations];
        
        // Sum up the squared deviation from mean
        
        for (int k=0; k<noOfSlots;k++){
        	for(int j = 0;j < noOfLocations;j++){
        		for(int i = 0;i < noOfDays * interval;i++)
        		{
        			sd [k][j] += (Error[k][i][j] - mean[k][j])*(Error[k][i][j] - mean[k][j]);
        		}

        	}
        }
        
        // Divide sum by no of data
        
        for(int i = 0;i < noOfSlots;i++)
        	for(int j = 0;j < noOfLocations;j++)
        		sd[i][j] = sd[i][j] / (noOfDays);
        
        // take the square root of the quotient
        
        for(int i = 0;i < noOfSlots;i++){
        	sdw.println("Standard deviaiton for slot "+ i+"\n");
        	for(int j = 0;j < noOfLocations;j++)
        		{
        			sd[i][j] = Math.sqrt(sd[i][j]);
        			sdw.println(sd[i][j]);       			
        		}
        
        }
        
        // Declare finalYbus to store admittance matrix
        
        double[][] finalYbus = new double[noOfBuses][noOfBuses];
        double[] fbus = new double[noOfBranches];
		double[] tbus = new double[noOfBranches];
		double[] reactance = new double[noOfBranches];
		
        // Find bus admittance matrix
        
        findBusAdmittance(finalYbus, fbus, tbus, reactance);
        
        // Declare inverse to store inverse of bus admittance matrix
        
		double[][] inverse = new double[noOfBuses][noOfBuses];
		
		// Convert bus admittance matrix from array to matrix form
		
		Matrix Ybus = new Matrix(finalYbus);
		
		// Calculate inverse
		
		Matrix matInverse = Ybus.inverse();
		
		// Convert inverse back into array form
		
		inverse = matInverse.getArray();
		
		// Declare variables to store bus data
		
		double[] loadBus = new double[loadBusCount];
		double[] genBus = new double[genBusCount];
		double[] finalPing = new double[noOfBuses - 1];
		double[] type = new double[noOfBuses];
		double[] Pgi = new double[noOfBuses];
		double[] Pli = new double[noOfBuses];
		
		try{
			
			// Declare input stream for reading file
			
			FileInputStream file = new FileInputStream(new File(gridPath));
			
			// Declare a workbook for opening .xls file
			
			HSSFWorkbook workbook = new HSSFWorkbook(file);
			
			// Select worksheet for reading
			
			HSSFSheet sheet = workbook.getSheetAt(1);
			
			// rcount keeps track of rows in the input file
			
			int rcount = -1;
			
			// Declare iterator object for worksheet
			
			Iterator<Row> rowit = sheet.iterator();
			
			// Iterate through each row
			
			while(rowit.hasNext()){
				
				// Read each row
				
				Row row = rowit.next();
				
				//Skip first row
				
				if(rcount == -1){
					rcount = 0;
				}
				else{
					
					// Declare iterator object for each row to read cell values
					
					Iterator<Cell> cellit = row.cellIterator();
					
					// ccount keeps track of each cell in row
					
					int ccount = 1; 
					while(cellit.hasNext()){
						
						// Read each cell
						
						Cell cell = cellit.next();
						
						// Store bus type
						
						if(ccount == 2){
							type[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						
						// Store power generated at each bus
						
						else if(ccount == 3){
							Pgi[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						
						// Store load demand at each bus
						
						else if(ccount == 4){							
							Pli[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						else
							ccount++;												
					}
					rcount++;
				}
			}
			
			int lindex = 0, gindex = 0; 
			for(int i = 0;i < noOfBuses;i++){
				
				// Store load bus numbers
				
				if(type[i] == 1){
					loadBus[lindex] = i + 1;
					lindex++;
				}
				
				// Store generator bus numbers
				
				if(type[i] == 2){
					genBus[gindex] = i + 1;
					gindex++;
				}
			}
						
		}
		
		// Handle exceptions occur at execution time
		
		catch(IOException z){
			System.out.println("IO error" + z);
		}
		
		// Generate power forecast for each of the solar location
		// Variable 'forecast' stores power forecast values
		
		double[][] forecast = new double[noOfSlots][noOfLocations];
		
		// Generate power forecast values randomly
		
		for(int i = 0;i < noOfLocations;i++){
			for (int j=0; j<noOfSlots;j++)
			{
				int k= ThreadLocalRandom.current().nextInt(1,15);
				if (Act[j][k][i]==0){
					forecast[j][i] = ThreadLocalRandom.current().nextDouble(0,1);
				}
				else
					forecast[j][i] = ThreadLocalRandom.current().nextDouble(Act[j][k][i]*0.5 , Act[j][k][i]*1.5);

			}
		}
		
		// Variable 'actual' stores actual power generated
		
		double[][] actual = new double[noOfSlots][noOfLocations];
		
		// Generate actual power generated randomly. More recent the forecast, lower the range of variation
		
		for(int i = 0;i < noOfLocations;i++){
			for (int j=0; j<noOfSlots;j++)
			{
				actual[j][i] = ThreadLocalRandom.current().nextDouble(forecast[j][i] * .5 , forecast[j][i] * 1.5);
			}

	
		}
		
		// Variable 'committed' stores committed power
		
		double[][] committed = new double[noOfSlots][noOfLocations];
		
		// Generate committed power from forecasted power and standard deviation of error
		
		for(int i = 0;i < noOfLocations;i++){
			p.println("Lcation  "+i+"\n");
			for (int j=0; j<noOfSlots;j++)
			{
				committed[j][i] = forecast[j][i] - sd[j][i];	
				p.println("Actuallly generated "+ actual[j][i] + "\t Committed "+committed[j][i]);
			}

		}
		
		// Declare selectLocations to store bus number corresponding to  locations selected by the optimizer
		
		double[][] selectLocations = new double[noOfSlots][noOfLocations];
		
		// Initialize selectLocations to zero
		
		for(int i = 0;i < noOfSlots;i++)
			Arrays.fill(selectLocations[i], 0);
		
		// Declare variable 'storage' to store battery power
		
		double[] storage = new double[noOfLocations];
		
		// Initialize battery power to zero
		
		Arrays.fill(storage, 0);
		
		
				
		// Generate random costs for renewable & non-renewable generators
		
		double[] rcost = new double[colcount];
		double[] nrcost = new double[noOfBuses];
		double rfixed = 100.0;
		double nrfixed = 250.0;
		
		
		for (int j=0; j< colcount; j++ )
		{
			rcost[j]= rfixed + ThreadLocalRandom.current().nextDouble( 500,1000 );
		}
		
		
		for (int j=0; j< noOfBuses; j++ )
		{
			nrcost[j]= nrfixed + ThreadLocalRandom.current().nextDouble(500, 1000);
		}
		
				
		
		// Declare a variable to store nonrenewable generation at each bus
		
		double[] nrGeneration = new double[noOfBuses];
		
		// Declare variables to store maximum and minimum power generation limit of non renewable generators
		
		double[] pgiMin = new double[noOfBuses];
		double[] pgiMax = new double[noOfBuses];
		
		// Load bus has no power generation
		
		for(int i = 0;i < noOfBuses;i++){
			if(type[i] == 2 || type[i] == 3)
				pgiMax[i] = 700;			
		}
		
 		try{
 			
 			// For each slot run optimization to get the generator schedule
 			
		    FileWriter fileWriter,sdata, ndata, cdata;
			
			fileWriter = new FileWriter("Output118.txt");
		    PrintWriter printWriter = new PrintWriter(fileWriter);
		    
		    sdata= new FileWriter ("datasolar.txt");
		    PrintWriter datasolar = new PrintWriter (sdata);
		    ndata= new FileWriter ("datanonren.txt");
		    PrintWriter datanonren = new PrintWriter (ndata);
		    cdata= new FileWriter ("datacost.txt");
		    PrintWriter datacost = new PrintWriter (cdata);
		    
 			
			for(int i = 0;i < noOfSlots;i++){
				printWriter.println("###########################################################");
				printWriter.println("SLOT " + (i + 1));
				printWriter.println("###########################################################");
				
				// Create a cplex object for defining optimization problem
				
				IloCplex cplex = new IloCplex();
				
				// Create optimization model
				
				createLPModel(cplex, rcost, nrcost, forecast[i], sd[i], inverse, pgiMax, loadBus, genBus, Pli, printWriter);
				
				// Export model to file sample.lp
				
				cplex.exportModel("sample.lp");
				
				// Solve optimization model
				
				solveAndPrint(cplex, rcost, nrcost,forecast[i], selectLocations[i], nrGeneration, printWriter, datasolar, datanonren, datacost);
				
				// Ping stores active power injection
				
				double[] Ping = new double[noOfBuses];
				
				// Calculate active power injection by considering only non renewable generation
				
				for(int j = 0;j < noOfBuses;j++)
					Ping[j] = nrGeneration[j] - Pli[j];
				
				// Update active power injection by considering renewable commitment
				
				for(int j = 0;j < noOfLocations;j++){
					if(selectLocations[i][j] == 1){
						Ping[(int)loadBus[j + 20] - 1] += committed[i][j];
					}
				}
				
				// Normalize active power injection by base power
				
				for(int j = 0;j < noOfBuses;j++)
					Ping[j] = Ping[j] / bmva;
				
				// Declare variable 'theeta' to store phase angle at each bus
				
				double[] theeta = new double[noOfBuses];
				
				// Initialize phase angles to zero
				
				for(int j = 0;j < noOfBuses;j++)
					theeta[j] = 0;
				
				// run DC load flow to calculate phase angles
				
				dcLoadFlow(inverse, Ping, theeta);
				
				/*
				System.out.println("committed values\n");
				for(int j = 0;j < noOfLocations;j++)
					System.out.print(committed[i][j] + "\t");
				System.out.println();
						
				System.out.println("actual values");
				for(int j = 0;j < noOfLocations;j++)
					System.out.print(actual[i][j] + "\t");
				System.out.println();
				*/
			}
			printWriter.close();
			datasolar.close();
			datanonren.close();
			datacost.close();
		}
 		
 		// Handle exceptions at run time
 		
		catch(IloException | IOException e){
			System.out.println("error" + e);
		}
 		
 		// System.currentTimeMillis() -  stores current time in milliseconds i.e, starting time 
		
 		long startTime   = System.currentTimeMillis();
 		
 		/* Update Storage */
 		
 		// FlowFlag indicates power deficiency
 		// Initialize flowFlag as zero
 		
 		int flowFlag = 0;
		
		for(int i = 0;i < noOfSlots;i++){
			
			// Declare a variable to store the deficiency at each solar location
			
			double[] deficiency = new double[noOfLocations];
			
			// Declare totalDef to store total deficiency
			// Initialize totalDef as zero
			
			double totalDef = 0;
			
			// newPli stores load at each bus
			
			double[] newPli = new double[noOfBuses]; 
			for(int j = 0;j < noOfBuses;j++){
				newPli[j] = Pli[j];
			}
			
			// newPgi stores generation at each bus
			
			double[] newPgi = new double[noOfBuses];
			
			// Update generation from non renewable generators
			
			for(int j = 0;j < noOfBuses;j++)
				newPgi[j] = nrGeneration[j];
			
			//System.out.println("Initial storage : SLOT " + (i + 1));
			
			//for(int j = 0;j < noOfLocations;j++)
				//System.out.print(storage[j] + "\t");
			//System.out.println();
			
			for(int j = 0;j < noOfLocations;j++){				
			
				// If j'th location is not selected then actual generation can be stored in battery
				
				if(selectLocations[i][j] == 0.0)
					storage[j] += actual[i][j];
				
				// If location has been selected
				
				else{
					
					// If renewable generation is more than committed generation
					
					if(actual[i][j] >= committed[i][j]){
						
						// Update storage
						
						storage[j] += actual[i][j] - committed[i][j];
						
						// Increase generation by committed value
						
						newPgi[(int)loadBus[j + 20] - 1] += committed[i][j];
					}
					
					// If renewable generation is less than committed generation
					
					else{
						
						// Update deficiency 
						
						deficiency[j] = committed[i][j] - actual[i][j];
						
						// If enough storage at location j to compensate deficiency
						
						if(storage[j] >= deficiency[j]){
							
							// Increase generation by committed value
							
							newPgi[(int)loadBus[j + 20] - 1] += committed[i][j];
							
							// Decrease storage by deficiency
							
							storage[j] -= deficiency[j];
							
							// set deficiency at location j as zero
							
							deficiency[j] = 0;							
						}
						
						// If available storage at location j is not enough to compensate deficiency
						
						else{
							
							// Update flowFlag
							
							flowFlag++;
							
							// Decrease deficiency by storage
							
							deficiency[j] -= storage[j];
							
							// Increase generation by storage
							
							newPgi[(int)loadBus[j + 20] - 1] += storage[j];
							
							// Set storage as zero
							
							storage[j] = 0;
						}
						
						// Update total deficiency
						
						totalDef += deficiency[j];
					}
				}
			}
			
			/* If there exists no deficiency
			
			if(flowFlag == 0){
				System.out.println("storage : SLOT " + (i + 1));
				for(int j = 0;j < noOfLocations;j++)
					System.out.print(storage[j] + "\t");
				System.out.println();
			}
			*/
			
			// If there exists deficiency
			
			if(flowFlag != 0){				
				//System.out.println("DEFICIENCY in SLOT " + (i + 1));
				
				// set flowFlag as zero
				
				flowFlag = 0;
				
				/* Update extra load if required */
				
				/*
				newPli[12] += extraLoad[i] / 2;
				newPli[13] += extraLoad[i] / 2;
				*/
				
				// Reduce load at some buses to compensate reduction in power generation
				
				newPli[57] -= totalDef / 2;
				newPli[58] -= totalDef / 2;
				
				// Declare theeta to store phase angles at each bus corresponding to the current state of the grid
				
				double[] theeta = new double[noOfBuses];
				
				// Initialize theeta to zero
				
				Arrays.fill(theeta, 0.0);
				
				// Declare a variable to store active power injection at each bus
				
				double[] newFinalPing = new double[noOfBuses];

				// Calculate active power injection from generation and load
				
				for(int j = 0;j < noOfBuses;j++){
					newFinalPing[j] = newPgi[j] - newPli[j];
					newFinalPing[j] /= bmva;
				}
				
				// Run DC load flow to calculate phase angle at each bus
				
				dcLoadFlow(inverse, newFinalPing, theeta);
				/*
				System.out.println("THEETA VALUES");
				for(int j = 0;j < noOfBuses - 1;j++)
					System.out.print(theeta[j] + "\t");
				System.out.println();
				
		
				System.out.println("theeta values");
				for(int j = 0;j < noOfBuses;j++){
					System.out.print(theeta[j] + "\t");
				}
				*/
				
				/*create graph for network flow*/
				
				// Declare a list of list to store adjacency matrix
				
				List<Integer>[] adjacency;
				
				// Initialize adjacency matrix
				
				adjacency = new ArrayList[vertices];
				for(int j = 0;j < vertices;j++)
					adjacency[j] = new ArrayList<Integer>();
				
				// Declare a variable to store capacity of each edge
				
				double[][] capacity = new double[vertices][vertices];
				
				// Declare a variable to store reactance at each bus
				
				double[][] react = new double[vertices][vertices];
				
				initializeGraph(adjacency, capacity, fbus, tbus, reactance, theeta, react);
				
				/*
				System.out.println("capacity values");
				for(int j = 0;j < vertices;j++){
					for(int k = 0;k < vertices;k++){
						System.out.print(capacity[j][k] + "\t");
					}
					System.out.println();
				}
				
				System.out.println("adjacency matrix");
				
				for(int j = 0;j < vertices;j++){
					for(int k = 0;k < adjacency[j].size();k++){
						System.out.print(adjacency[j].get(k) + "\t");
					}
					System.out.println();
				}
				*/
				/* add edges to source vertex */
				for(int j = 0;j < noOfLocations;j++){
					if(storage[j] > 0){
						capacity[0][(int)loadBus[j + 20]] = storage[j];
						adjacency[0].add((int)loadBus[j + 20]);
					}					
				}
				/* add edges to sink vertex */
				adjacency[57].add(vertices - 1);
				capacity[57][vertices - 1] = totalDef / 2;
				adjacency[58].add(vertices - 1);
				capacity[58][vertices - 1] = totalDef / 2;
				
				/*
			
				System.out.println("capacity values");
				for(int j = 0;j < vertices;j++){
					for(int k = 0;k < vertices;k++){
						System.out.print(capacity[j][k] + "\t");
					}
					System.out.println();
				}
				
				System.out.println("adjacency matrix");
				
				for(int j = 0;j < vertices;j++){
					for(int k = 0;k < adjacency[j].size();k++){
						System.out.print(adjacency[j].get(k) + "\t");
					}
					System.out.println();
				}
				
				/* run EDMONDS-KARP algorithm */
				
				double[][] flow = new double[vertices][vertices];
				int source = 0, sink = vertices - 1;
				while(true){
					int[] parent = new int[vertices];
					Arrays.fill(parent, -1);
					parent[source] = source;
					double[] nodeFlow = new double[vertices];
					nodeFlow[source] = Double.MAX_VALUE;
					Queue<Integer> queue = new LinkedList<Integer>();
					queue.offer(source);
					LOOP:
						while(!queue.isEmpty()){
							int poped = queue.poll();
							for(int adj : adjacency[poped]){
								if(capacity[poped][adj] - flow[poped][adj] > 0 && parent[adj] == -1){
									parent[adj] = poped;
									nodeFlow[adj] = Math.min(capacity[poped][adj] - flow[poped][adj], nodeFlow[poped]);
									if(adj != sink)
										queue.offer(adj);
									else{
										//System.out.println("\nsink reached");
										while(parent[adj] != adj){
											poped = parent[adj];
											flow[poped][adj] += nodeFlow[sink];
											flow[adj][poped] -= nodeFlow[sink];
											adj = poped;
										}
										break LOOP;
									}
								}
							}
						}
					/* if no augmenting path exists */
					if(parent[vertices - 1] == -1){
						double sum = 0;
						for(double f : flow[source]){
							sum += f;
						}
						//System.out.println();
						//System.out.println("total flow = " + sum + "\t total deficiency = " + totalDef +"\n");
						if(Math.abs(sum - totalDef) < 0.1){
							//System.out.println("successful");							
						}
						
						/*print flow values
						System.out.println("flow values");
						for(int j = 0;j < vertices;j++){
							for(int k = 0;k < vertices;k++){
								if(flow[j][k] > 0){
									System.out.println(j + " -- "  + k + " = " + flow[j][k]);
								}
							}
						}
						*/
						
						/*reactance change required*/
						//System.out.println("reactance change required");
						for(int j = 0;j < vertices;j++){
							for(int k = 0;k < vertices;k++){
								if(flow[j][k] > 0 && j != 0 && k != 0 && j != vertices - 1 && k != vertices - 1){
									double x_1 = (theeta[j - 1] - theeta[k - 1]) * bmva;
									double x_2 = react[j][k];
									double x_3 = x_1 / x_2;
									double changeReact = (x_2 * (flow[j][k] + x_3) - x_1) / (flow[j][k] + x_3);
									//System.out.println(j + " -- " + k + " = " + changeReact);
								}
							}
						}
						/*update storage*/
						for(int j = 0;j < noOfLocations;j++){
							if(flow[source][(int)loadBus[j + 20]] > 0){
								storage[j] -= flow[source][(int)loadBus[j + 20]];
							}
						}
						
						/*
						System.out.println("storage : SLOT " + (i + 1));
						for(int j = 0;j < noOfLocations;j++)
							System.out.print(storage[j] + "\t");
						System.out.println();
						*/
						break;
						
					}
				}
			}
		}
				
 		// Stores the end time
 		
 		long endTime   = System.currentTimeMillis();
 		
 		// Calculate time duration from start time and end time
 		
 		long totalTime = endTime - startTime;
 		
 		// Print execution time 
 		
 		System.out.println("TOTAL TIME IN MILLISECONDS = " + totalTime);
 		
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	/* Method solveAndPrint() - runs the optimization model
	 * cplex - is the Cplex object defining the model
	 * */
	
	static void solveAndPrint(IloCplex cplex,double rcost[], double nrcost[],double[]forecast, double[] selectLocations, double[] nrGeneration, PrintWriter printWriter, PrintWriter solardata, PrintWriter nonrendata, PrintWriter costdata){

		try
		{			
			
			// If a solution exists
			
			if(cplex.solve()){
		      
				// Create a matrix representing result
				
		        IloLPMatrix lp = (IloLPMatrix)cplex.LPMatrixIterator().next();
		        
		        // Get decision variable values and stores it in variable 'x'
		        
		        double[] x = cplex.getValues(lp);
		        
		        // nvars - stores the number of decision variables
		        
		        int nvars = x.length;
		        int numsolar =0;
		        int xover=0;
		        double nonren=0, totalrcost=0, totalnrcost=0;
		        		        

			    	
				
		        		        
		        // Store and print result of optimization
		        // Avoid last decision variable since its value is always 1
		        
		        for (int j = 0;j < nvars - 1;++j){
		        	
		        	
		        	// Store selected locations
			        // 0 indicates locations has not been selected, otherwise selected
		        	
		        	if(j < colcount){
		        		if(x[j] == -0.0)
			        		selectLocations[j] = 0.0;
			        	else
			        		selectLocations[j] = x[j];			        	
		        	}
		        	
		        	// Store non renewable power generation
		        	
		        	else{
		        		nrGeneration[j - colcount] = x[j] * bmva;
		        	}
		        	
		        	// print the result 
		        	if (x[j]>0)
		        	{
		        		if (j<colcount)
		        		{
		        			printWriter.println("\t x["+j+"]    " + x[j]);
		        			totalrcost+= x[j]*rcost[j]*forecast[j];
		        			numsolar++;
		        		}
		        		else
		        			
		        		{
		        			if (xover==0)
		        			{
		        				printWriter.println("\n\n");
		        				xover=1;
		        			}
		        			printWriter.println("\t y["+(j-colcount)+"]    " + x[j]);
		        			nonren+=x[j]*bmva;
		        			totalnrcost+= x[j]*bmva*nrcost[j-colcount];
		        		}
		        	}
		        }
		        
		        
		        printWriter.println(" Locations commiting solar power in this slot = " + numsolar);
		        printWriter.println("\n Non-renewable power commited in this slot = " + nonren);
		        printWriter.println("\n Solar power commited in this slot = " + (demand - nonren));
		        
		        
		        //nonrendata.println ("Non-renewable power  "+(nonren)+"\t cost  "+(totalnrcost));
		        //solardata.println ("Renewable power  "+(demand -nonren)+"\t cost  "+(totalrcost));
		        costdata.println ((totalnrcost+totalrcost)/1e6);
		        
		        
		        solardata.println ((demand-nonren));
		        nonrendata.println (nonren);
		        
		        
		    }
			
			// No solution exists
			
			else
				System.out.println("not solved");	     
		}
		
		// Handles run time exceptions
		
		catch(Exception e){System.out.println("EXXXXXCEPTION OCCURED" + e);}
	  }
	
	/* Method createLPModel() - Defines a Mixed Integer Linear Programming Optimization Model 
	 * model - is the Cplex object defining the model
	 * forecast - power forecast at different solar locations
	 * sd - standard deviation of forecast error at different locations on current time slot
	 * inverse - bus admittace inverse matrix
	 * pgiMax - maximum power generation limit of nonrenewable generators
	 * loadBus - bus number of load buses
	 * genBus - bus number of generator buses 
	 * Pli - load at each bus
	 * */
	
	static void createLPModel(IloMPModeler model,double[] rcost, double nrcost[], double[]forecast, double[] sd,  double[][] inverse, double[] pgiMax, double[] loadBus, double[] genBus, double[] Pli, PrintWriter printWriter){
		
		try{				
			
			// Create a matrix representation for model
			
			IloLPMatrix lp = model.addLPMatrix();
			
			// Declare finalPli to store normalized load values at each bus
			
			double[] finalPli = new double[noOfBuses];
			
			// Normalize load by base power bmva
			
			for(int i = 0;i < noOfBuses;i++)
				finalPli[i] = Pli[i] / bmva;
			
			// Inject more load to increase power demand which can be met by solar plants
			
		//	finalPli[12] += 50 / bmva;
		//	finalPli[13] += 50 / bmva;
			
			// Declare finalPgiMax to store normalized maximum power generation limit at each nonrenewable generator
			
			double[] finalPgiMax = new double[noOfBuses];
			
			// Normalize maximum power generation limit by base power bmva
			
			for(int i = 0;i < noOfBuses;i++)
				finalPgiMax[i] = pgiMax[i] / bmva;
			
			// Declare variable lb to store lower bound of each decision variable
			
			double[] lb = new double[colcount + noOfBuses + 1];
			
			// Declare variable ub to store upper bound of each decision variable
			
			double[] ub = new double[colcount + noOfBuses + 1];
			
			// Declare variable type to store type of each variable
			
			IloNumVarType[] type = new IloNumVarType[colcount + noOfBuses + 1];
			
			// Define decision variables corresponding to selection of solar locations
			
			for(int i = 0;i < colcount;i++){
				
				// Lower bound of selection variable set to 0
				
				lb[i] = 0.0;
				
				// Upper bound of selection variable set to 1
				
				ub[i] = 1.0;
				
				// Set type as float
				
				type[i] = IloNumVarType.Float;
			}
			
			// Define decision variables corresponding to nonrenewable generators
			
			for(int i = 0;i < noOfBuses;i++){
				
				// Lower bound set to 0
				
				lb[colcount + i] = 0;
				
				// Upper bound set to maximum generation limit
				
				ub[colcount + i] = finalPgiMax[i];
				
				// Set type as floating point
				
				type[colcount + i] = IloNumVarType.Float;
			}
			
			// Define a variable to handle constant part in the constraints corresponding to load flow equations
			
			// Set lower bound as 1
			
			lb[colcount + noOfBuses] = 1;
			
			// Set upper bound as 1
			
			ub[colcount + noOfBuses] = 1;
			
			// Set type as integer
			
			type[colcount + noOfBuses] = IloNumVarType.Int;
			
			// Create decision variabes for model
			
			IloNumVar[] x  = model.numVarArray(model.columnArray(lp, colcount + noOfBuses + 1), lb, ub, type);
			
			// Set variable names as x1, x2, ... for selection variables 
			
			for(int i = 0;i < colcount;i++){
				x[i].setName("x" + i);
			}
			
			// Set variable names as y1, y2, ... for nonrenewable power injection variables
			
			for(int i = 0;i < noOfBuses;i++)
				x[colcount + i].setName("y" + i);
			
			// Set variable name as z for constant part
			
			x[colcount + noOfBuses].setName("z");
						
			// Calculate constant part 
			
			double[] constant = new double[noOfBuses];		
			for(int i = 0;i < noOfBuses;i++)
				constant[i] = 0.0;
			for(int i = 0;i < noOfBuses;i++){
				for(int j = 0;j < noOfBuses;j++){
					constant[i] += inverse[i][j] * finalPli[j] * -1; 
				}
			}
			
			// Declare variable 'row' to store constraints
			
			IloRange[] row = new IloRange[2 * noOfBuses + 1];
			
			// Add constraints for dc load flow
			
			// For each bus in the grid
			
			for(int i = 1;i < noOfBuses;i++){
				
				// Create an expression object to store constraints
				
				IloLinearNumExpr constraintExpr = model.linearNumExpr();
				
				// Add terms corresponding to nonrenewable power injection
				
				for(int j = 0;j < noOfBuses;j++){
					constraintExpr.addTerm(inverse[i][j], x[j + colcount]);				
				}
				
				// Add terms corresponding to solar location with renewable power injection
				// Here we inject power at all load bus locations
				
				for(int j = 0;j < colcount;j++){
					constraintExpr.addTerm(inverse[i][(int)loadBus[j + 20] - 1] * ((forecast[j] - sd[j]) / bmva), x[j]);				
				}
				
				// Add constant part in the constraint
				
				constraintExpr.addTerm(constant[i], x[colcount + noOfBuses]);
				
				// Add constraints to the model
				// Set upper limit of bus angle as upper limit of constraint
				
				row[i - 1] = model.addLe(constraintExpr, thetaMax);
				
				// Set lower limit of bus angle as lower limit of constraint
				
				row[i + noOfBuses - 1] = model.addGe(constraintExpr, thetaMin);
			}
			
			// Add selection constraint x1 + x2 + .... + xn = number of locations selected			
			// Create an expression object to store constraints
			
			/*IloLinearNumExpr constraintExpr = model.linearNumExpr();		
			for(int i = 0;i < colcount;i++)
				constraintExpr.addTerm(1.0, x[i]);
			constraintExpr.addTerm(1.0, x[colcount + noOfBuses]);
			
			// RHS is selected + 1 since a decision variable corresponding to constant has been added
			
			row[2 * noOfBuses - 2] = model.addEq(constraintExpr, selected + 1);
			*/
			// Demand supply constraint			
			// Create an expression object to store constraints
			
			IloLinearNumExpr constraintExprNew = model.linearNumExpr();
			
			// Add constraint corresponding to renewable supply
			
			for(int i = 0;i < colcount;i++)
				constraintExprNew.addTerm((forecast[i] - sd[i]), x[i]);
			
			// Add constraint corresponding nonrenewable supply
			
			for(int i = 0;i < noOfBuses;i++)
				constraintExprNew.addTerm(bmva, x[colcount + i]);
			
			// Total supply must meet the demand
			
			row[2 * noOfBuses - 1] = model.addEq(constraintExprNew, demand);
			
			// Add slack bus constraint
			
			// Create an expression object to store constraints
			
			IloLinearNumExpr constraintExprSlack = model.linearNumExpr();
			
			// Add terms corresponding to nonrenewable power injection
			
			for(int j = 0;j < noOfBuses;j++){
				constraintExprSlack.addTerm(inverse[0][j], x[j + colcount]);	
			}
			
			// add terms corresponding to solar location with renewable power injection
			// Here we inject power at all load bus locations
			
			for(int j = 0;j < colcount;j++){
				constraintExprSlack.addTerm(inverse[0][(int)loadBus[j + 20] - 1] * ((forecast[j] - sd[j]) / bmva), x[j]);				
			}
			
			// Add constant term
			
			constraintExprSlack.addTerm(constant[0], x[colcount + noOfBuses]);
			
			// Slack bus phase angle is set to zero
			
			row[2 * noOfBuses] = model.addEq(constraintExprSlack, 0);
						
			// Create an expression object to store objective function
			
			IloLinearNumExpr objExpr = model.linearNumExpr();
			
			
			
			// Add terms corresponding to renewable generators
			
			for (int i = 0; i < colcount; ++i){
		    	objExpr.addTerm((forecast[i] - sd[i])*rcost[i], x[i]);
		    }
		    
		    for (int i = 0; i < noOfBuses; ++i){
		    	objExpr.addTerm(nrcost[i]*bmva, x[colcount+i]);
		    }
		    
		    // Maximize the objective function
		    
		    IloObjective obj = model.minimize(objExpr);		    
		    model.add(obj);
		    
		    
		    printWriter.println("demand = "+demand);
		    /*
		    System.out.println("load at each bus");
		    for(int i = 0;i < noOfBuses;i++)
		    	System.out.println(finalPli[i] * bmva);
		    	
		    System.out.println("power forecast values\n");
			for(int i = 0;i < noOfLocations;i++)
				System.out.print(forecast[i] + "\t");
				
			System.out.println("\n\npower committed values\n");
			for(int i = 0;i < noOfLocations;i++)
				System.out.print(forecast[i] - sd[i] + "\t"); //assume k = 1
			*/
		}
		catch(Exception e){System.out.println("exxxxception occured" + e);}
		
	}
	
	
	/* Method findBusAdmittance() - calculate bus admittance matrix corresponding to IEEE 14 bus system 
	 * YbusImaginary - imaginary part of bus admittance matrix
	 * fbus - bus representing beginning of a line
	 * tbus - bus representing ending of a line
	 * reactance - reactance at each line 
	 * */
	
	static void findBusAdmittance(double[][] YbusImaginary, double[] fbus, double[] tbus, double[] reactance){
		try{
						
			// Declare input stream for reading file
			
			FileInputStream loadfile = new FileInputStream(new File(gridPath));
			
			// Declare a workbook for opening .xls file
			
			HSSFWorkbook workbook = new HSSFWorkbook(loadfile);
			
			// Select sheet number 1 for reading
			
			HSSFSheet sheet = workbook.getSheetAt(0);
			
			// rcount - keeps track of the row number
			
			int rcount = -1;
			
			// Declare variables to store bus numbers and parameters like resistance, reactance etc.
			
			double[] resistance = new double[noOfBranches];
			double[] admittance = new double[noOfBranches];
			double[] tap = new double[noOfBranches];
			
			// Iterate through each row
			// Declare an Iterator object for worksheet to read rows
			
			Iterator<Row> rowit = sheet.iterator();
			while(rowit.hasNext()){
				
				// Read each row 
				
				Row row = rowit.next();
				
				// Do not process first row since it contains only field names
				
				if(rcount == -1)
					rcount = 0;
				
				// Process all other rows				
				
				else{					
					
					// Declare an Iterator object for each row to read cell values 
					
					Iterator<Cell> cellit = row.cellIterator();
					
					// ccount - keeps track of column numbers
					
					int ccount = 1; 
					while(cellit.hasNext()){
						Cell cell = cellit.next();
						
						// Store beginning bus number of each transmission line
						
						if(ccount == 1){
							fbus[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						
						// Store ending bus number of each transmission line
						
						else if(ccount == 2){
							tbus[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						
						// Store resistance of each transmission line
						
						else if(ccount == 3){							
							resistance[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						
						// Store reactance of each transmission line
						
						else if(ccount == 4){							
							reactance[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						
						// Store ground admittance of each transmission line
						
						else if(ccount == 5){							
							admittance[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						
						// Store transformer tap ratios
						
						else{
							tap[rcount] = cell.getNumericCellValue();
							ccount++;
						}												
					}
					rcount++;
				}
			}
			
			// Create a column matrix to store impedance in complex format*/
			
			Complex[] zmatrix = new Complex[noOfBranches];
			
			// Declare variables to store multiplicative inverse of complex impedance and admittance 
			
			Complex[] inverse = new Complex[noOfBranches];
			Complex[] compAdmittance = new Complex[noOfBranches];
			for(int i = 0;i < noOfBranches;i++){
				
				// Create complex impedance
				
				zmatrix[i] = new Complex(resistance[i], reactance[i]);
				
				// Calculate multiplicative inverse
				
				inverse[i] = zmatrix[i].reciprocal();
				
				// Complex number with admittance as imaginary part
				
				compAdmittance[i] = new Complex(0.0, admittance[i]);
			}
			
			// Calculate bus admittance matrix 
			
			Complex[][] Y_bus = new Complex[noOfBuses][noOfBuses];
			for(int i = 0;i < noOfBuses;i++)
				for(int j = 0;j < noOfBuses;j++)
					Y_bus[i][j] = new Complex(0.0, 0.0);
			for(int i = 0;i < noOfBuses;i++){
				for(int j = 0;j < noOfBranches;j++){
					if(tap[j] == 1.0){
						if(fbus[j] == i + 1 || tbus[j] == i + 1)
							Y_bus[i][i] = Y_bus[i][i].add(inverse[j].add(compAdmittance[j]));						
					}
				}
			}
			for(int i = 0;i < noOfBranches;i++){
				Y_bus[(int)fbus[i] - 1][(int)tbus[i] - 1] = Y_bus[(int)fbus[i] - 1][(int)tbus[i] - 1].subtract(inverse[i]);
				Y_bus[(int)tbus[i] - 1][(int)fbus[i] - 1] = Y_bus[(int)fbus[i] - 1][(int)tbus[i] - 1]; 
			}

			// Stores imaginary part of Y_bus since resistance is assumed as zero
			
			for(int i = 0;i < noOfBuses;i++)
				for(int j = 0;j < noOfBuses;j++)
					YbusImaginary[i][j] = Y_bus[i][j].getImaginary();
			
		}
		
		// Handle exceptions occur during execution
		
		catch(IOException io){System.out.println("IO error" + io);}
	}
	
	/* Method dcLoadFlow() - run DC load flow to get phase angles at each bus
	 * inverse - inverse of bus admittance matrix
	 * Ping - active power injection at each bus
	 * theeta - phase angles at each bus
	 */

	static void dcLoadFlow(double[][] inverse, double[] Ping, double[] theeta){
		for(int i = 0;i < noOfBuses - 1;i++){
			for(int j = 0;j < noOfBuses - 1;j++){
				theeta[i] += inverse[i][j] * Ping[j];
			}
		}
	}
	
	/* Method initializeGraph() - Create a graph corresponding to the electric grid topology
	 * adjacency - adjacency list representation of graph 
	 * capacity - capacity of each transmission line
	 * fbus - bus representing beginning of a line
	 * tbus - bus representing ending of a line
	 * reactance - reactance at each line
	 * theeta - phase angle at each bus
	 * react - reactance at each line 
	 */
	
	static void initializeGraph(List<Integer>[] adjacency, double[][] capacity, double[] fbus, double[] tbus, double[] reactance, double[] theeta, double[][] react){
		for(int i = 0;i < noOfBranches;i++){
			if(theeta[(int)(fbus[i] - 1)] > theeta[(int)(tbus[i] - 1)]){
				adjacency[(int)fbus[i]].add((int)tbus[i]);
				/*calculate current flow as ((theeta_1 - theeta_2) / x)*/
				double currentFlow = (theeta[(int)(fbus[i] - 1)] - theeta[(int)(tbus[i] - 1)]) * bmva / reactance[i];
				/*calculate maximum flow by decreasing reactance by 30 % using FACTS*/
				double maxFlow = (theeta[(int)(fbus[i] - 1)] - theeta[(int)(tbus[i] - 1)]) * bmva / (reactance[i] - .8 * reactance[i]);
				/*update edge capacity*/
				capacity[(int)fbus[i]][(int)tbus[i]] = maxFlow - currentFlow;
				react[(int)fbus[i]][(int)tbus[i]] = reactance[i];
			}
			if(theeta[(int)(fbus[i] - 1)] < theeta[(int)(tbus[i] - 1)]){
				adjacency[(int)tbus[i]].add((int)fbus[i]);
				double currentFlow = (theeta[(int)(tbus[i] - 1)] - theeta[(int)(fbus[i] - 1)]) * bmva / reactance[i];
				double maxFlow = (theeta[(int)(tbus[i] - 1)] - theeta[(int)(fbus[i] - 1)]) * bmva / (reactance[i] - .8 * reactance[i]);
				capacity[(int)tbus[i]][(int)fbus[i]] = maxFlow - currentFlow;
				react[(int)tbus[i]][(int)fbus[i]] = reactance[i];
			}
		}
	}
}

