/*************************************************************
PROGRAM TO MAXIMIZE POWER COMMITMENT FROM SOLAR LOCATIONS 
USES IEEE 14 BUS SYSTEM
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import org.apache.commons.math3.complex.Complex;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

public class original14 {

	/* Static variable declarations */
	
	static int noOfDays = 15, noOfLocations = 9, noOfSlots = 4, noOfBuses = 14, noOfBranches = 20, loadBusCount = 9, genBusCount = 5, selected = 3, colcount = 9;
	
	// There are 12 time intervals in a slot of 3 hours i.e, 15 minutes interval duration
	
	static int interval = 12;  
	static double bmva = 100.0, thetaMin = -.785398, thetaMax = .785398;
	
	// Total demand in a basic IEEE 14 bus system without solar power injection is 259 MW
	
	static double  demand = 359;
	
	// Declare path for grid data file
	
	static String gridPath = "C:\\Users\\Arghya\\Documents\\BTP\\data\\data\\loadflowdata.xls";
	
	/* Main method */
	
	public static void main(String[] args) {
		
		// csvFilePrefix stores path of history solar data
		
		String csvFilePrefix = "C:\\Users\\Arghya\\Documents\\BTP\\data\\data\\solardata\\";
		
		// Declare variables to store actual weather data corresponding to different time slots
		// Here we consider 4 time slots from 6:00 am to 6:00 pm, each of 3 hours duration
		
		double[][] sixToNineAct = new double[noOfDays * interval][noOfLocations];
		double[][] nineToTwelveAct = new double[noOfDays * interval][noOfLocations];
		double[][] twelveToThreeAct = new double[noOfDays * interval][noOfLocations];
		double[][] threeToSixAct = new double[noOfDays * interval][noOfLocations];
		
		// Declare variables to store predicted weather data corresponding to different time slots
		
		double[][] sixToNinePre = new double[noOfDays * interval][noOfLocations];
		double[][] nineToTwelvePre = new double[noOfDays * interval][noOfLocations];
		double[][] twelveToThreePre = new double[noOfDays * interval][noOfLocations];
		double[][] threeToSixPre = new double[noOfDays * interval][noOfLocations];
		
		String line = "";
        String cvsSplitBy = "	";
        int sixNineIndex = 0, nineTwelveIndex = 0, twelveThreeIndex = 0, threeSixIndex = 0;
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
            		
            		else if(count > 6 * interval){
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
    	        			
    	        			// Store values into corresponding actual prediction variables
    	        			
    	        			if(count >= 2 * interval + 1 && count <= 3 * interval)
    	        				sixToNineAct[sixNineIndex ++][0] = power;
    	        			else if(count >= 3 * interval + 1 && count <= 4 * interval)
    	        				nineToTwelveAct[nineTwelveIndex ++][0] = power;
    	        			else if(count >= 4 * interval + 1 && count <= 5 * interval)
    	        				twelveToThreeAct[twelveThreeIndex ++][0] = power;
    	        			else 
    	        				threeToSixAct[threeSixIndex ++][0] = power;
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
        		
        		change = ThreadLocalRandom.current().nextDouble(5, 30); //made modification
        		sixToNineAct[j][i] = sixToNineAct[j][0] + change;
        		change = ThreadLocalRandom.current().nextDouble(20, 50); //made modification
        		nineToTwelveAct[j][i] = nineToTwelveAct[j][0] + change;
        		change = ThreadLocalRandom.current().nextDouble(20, 50); //made modification
        		twelveToThreeAct[j][i] = twelveToThreeAct[j][0] + change;
        		change = ThreadLocalRandom.current().nextDouble(5, 30);
        		threeToSixAct[j][i] = threeToSixAct[j][0] + change;
        	}
        }
        
        // Generate forecast data randomly for all locations from corresponding actual data
        
        for(int i = 0;i < noOfDays * interval;i++){
        	for(int j = 0;j < noOfLocations;j++){
        		
        		// sixNineLowPer stores lower bound for random value range
        		// minimum lower bound is zero
        		
        		double sixNineLowPer = (sixToNineAct[i][j] != 0) ? sixToNineAct[i][j] - sixToNineAct[i][j] * .1 : 0;
        		
        		// sixNineUpPer stores upper bound for random value range
        		// minimum upper bound is 5 MW
        		
        		double sixNineUpPer = (sixToNineAct[i][j] != 0) ? sixToNineAct[i][j] + sixToNineAct[i][j] * .1 : 5;
        		
        		// Generate forecast value from lower bound and upper bound
        		
        		sixToNinePre[i][j] = ThreadLocalRandom.current().nextDouble(sixNineLowPer, sixNineUpPer);
        		
        		// Generate forecast value for all slots
        		
        		double nineTwelveLowPer = (nineToTwelveAct[i][j] != 0) ? nineToTwelveAct[i][j] - nineToTwelveAct[i][j] * .1 : 0;
        		double NineTwelveUpPer = (nineToTwelveAct[i][j] != 0) ? nineToTwelveAct[i][j] + nineToTwelveAct[i][j] * .1 : 5;
        		nineToTwelvePre[i][j] = ThreadLocalRandom.current().nextDouble(nineTwelveLowPer, NineTwelveUpPer);
        		double twelveThreeLowPer = (twelveToThreeAct[i][j] != 0) ? twelveToThreeAct[i][j] - twelveToThreeAct[i][j] * .1 : 0;
        		double twelveThreeUpPer = (twelveToThreeAct[i][j] != 0) ? twelveToThreeAct[i][j] + twelveToThreeAct[i][j] * .1 : 5;
        		twelveToThreePre[i][j] = ThreadLocalRandom.current().nextDouble(twelveThreeLowPer, twelveThreeUpPer);
        		double threeSixLowPer = (threeToSixAct[i][j] != 0) ? threeToSixAct[i][j] - threeToSixAct[i][j] * .1 : 0;
        		double threeSixUpPer = (threeToSixAct[i][j] != 0) ? threeToSixAct[i][j] + threeToSixAct[i][j] * .1 : 5;
        		threeToSixPre[i][j] = ThreadLocalRandom.current().nextDouble(threeSixLowPer, threeSixUpPer);
        	}
        }
        
        // Declare variables to store forecast error for all time slots
        
        double[][] sixNineError = new double[noOfDays * interval][noOfLocations];
        double[][] nineTwelveError = new double[noOfDays * interval][noOfLocations];
        double[][] twelveThreeError = new double[noOfDays * interval][noOfLocations];
        double[][] threeSixError = new double[noOfDays * interval][noOfLocations];
        
        // calculate forecast error by subtracting forecasted value from actual value
        
        for(int i = 0;i < noOfDays * interval;i++){
        	for(int j = 0;j < noOfLocations;j++){
        		sixNineError[i][j] = sixToNineAct[i][j] - sixToNinePre[i][j];
        		nineTwelveError[i][j] = nineToTwelveAct[i][j] - nineToTwelvePre[i][j];
        		twelveThreeError[i][j] = twelveToThreeAct[i][j] - twelveToThreePre[i][j];
        		threeSixError[i][j] = threeToSixAct[i][j] - threeToSixPre[i][j];
        	}
        }
        
        // Declare variable to store mean forecast error values
        
        double[][] mean = new double[noOfSlots][noOfLocations];
        
        // Initialize mean as zero
        
        for(int i = 0;i < noOfSlots;i++)
        	Arrays.fill(mean[i], 0.0);
        
        // Sum error values 
        
        for(int i = 0;i < noOfDays * interval;i++){
        	for(int j = 0;j < noOfLocations;j++){
        		mean[0][j] += sixNineError[i][j];
        		mean[1][j] += nineTwelveError[i][j];
        		mean[2][j] += twelveThreeError[i][j];
        		mean[3][j] += threeSixError[i][j];
        	}
        }
        
        // Calculate mean by dividing sum by no of data
        
        for(int i = 0;i < noOfSlots;i++){
        	for(int j = 0;j < noOfLocations;j++){
        		mean[i][j] = mean[i][j] / (noOfDays * interval);
        	}
        }
        
        // Find forecast error standard deviation for all slots and locations
        // Declare a variable to store standard deviation
        
        double[][] sd = new double[noOfSlots][noOfLocations];
        
        // Sum up the squared deviation from mean
        
        for(int i = 0;i < noOfDays * interval;i++){
        	for(int j = 0;j < noOfLocations;j++){
        		sd[0][j] += (sixNineError[i][j] - mean[0][j]) * (sixNineError[i][j] - mean[0][j]);
        		sd[1][j] += (nineTwelveError[i][j] - mean[1][j]) * (nineTwelveError[i][j] - mean[1][j]);
        		sd[2][j] += (twelveThreeError[i][j] - mean[2][j]) * (twelveThreeError[i][j] - mean[2][j]);
        		sd[3][j] += (threeSixError[i][j] - mean[3][j]) * (threeSixError[i][j] - mean[3][j]);
        	}
        }
        
        // Divide sum by no of data
        
        for(int i = 0;i < noOfSlots;i++)
        	for(int j = 0;j < noOfLocations;j++)
        		sd[i][j] = sd[i][j] / (noOfDays * interval);
        
        // take the squre root of the quotient
        
        for(int i = 0;i < noOfSlots;i++)
        	for(int j = 0;j < noOfLocations;j++)
        		sd[i][j] = Math.sqrt(sd[i][j]);
        
        // Declare finalYbus to store admittance matrix
        
        double[][] finalYbus = new double[noOfBuses][noOfBuses];
        
        // Find bus admittance matrix
        
        findBusAdmittance(finalYbus);
        
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
			
			HSSFSheet sheet = workbook.getSheetAt(0);
			
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
						
						else if(ccount == 5){
							Pgi[rcount] = cell.getNumericCellValue();
							ccount++;
						}
						
						// Store load demand at each bus
						
						else if(ccount == 7){							
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
			
			// Ping stores active power injection
			
			double[] Ping = new double[noOfBuses];
			for(int i = 0;i < noOfBuses;i++)
				Ping[i] = Pgi[i] - Pli[i];
			
			// finalPing remove slack values
			
			for(int i = 0;i < noOfBuses - 1;i++)
				finalPing[i] = Ping[i + 1];
			
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
			forecast[0][i] = ThreadLocalRandom.current().nextDouble(sixToNineAct[9][i] - sixToNineAct[9][i] * .1 , sixToNineAct[9][i] + sixToNineAct[9][i] * .1);
			forecast[1][i] = ThreadLocalRandom.current().nextDouble(nineToTwelveAct[9][i] - nineToTwelveAct[9][i] * .1 , nineToTwelveAct[9][i] + nineToTwelveAct[9][i] * .1);
			forecast[2][i] = ThreadLocalRandom.current().nextDouble(twelveToThreeAct[9][i] - twelveToThreeAct[9][i] * .1 , twelveToThreeAct[9][i] + twelveToThreeAct[9][i] * .1);
			forecast[3][i] = ThreadLocalRandom.current().nextDouble(threeToSixAct[9][i] - threeToSixAct[9][i] * .1 , threeToSixAct[9][i] + threeToSixAct[9][i] * .1);
		}
		
		// Declare variables to store maximum and minimum power generation limit of non renewable generators
		
		double[] pgiMin = new double[noOfBuses];
		double[] pgiMax = new double[noOfBuses];
		
		// Load bus has no power generation
		
		for(int i = 0;i < noOfBuses;i++){
			if(type[i] == 2 || type[i] == 3)
				pgiMax[i] = 400;			
		}
		
		// System.currentTimeMillis() -  stores current time in milliseconds i.e, starting time 
		
		long startTime   = System.currentTimeMillis();
 		try{
 			
 			// For each slot run optimization to get the generator schedule
 			
			for(int i = 0;i < noOfSlots;i++){
				System.out.println("###########################################################");
				System.out.println("SLOT " + (i + 1));
				System.out.println("###########################################################");
				
				// Create a cplex object for defining optimization problem
				
				IloCplex cplex = new IloCplex();
				
				// Create optimization model
				
				createLPModel(cplex, forecast[i], sd[i], inverse, pgiMax, loadBus, genBus, Pli);
				
				// Export model to file sample.lp
				
				cplex.exportModel("sample.lp");
				
				// Solve optimization model
				
				solveAndPrint(cplex);				
			}
		}
 		
 		// Handle exceptions at run time
 		
		catch(IloException e){
			System.out.println("error" + e);
		}
 		
 		// Stores the end time
 		
 		long endTime   = System.currentTimeMillis();
 		
 		// Calculate time duration from start time and end time
 		
 		long totalTime = endTime - startTime;
 		
 		// Print execution time 
 		
 		System.out.println("TOTAL TIME IN MILLISECONDS = " + totalTime);
	}
	
	/* Method solveAndPrint() - runs the optimization model
	 * cplex - is the Cplex object defining the model
	 * */
	
	static void solveAndPrint(IloCplex cplex){

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
		        
		        // Print decision variables values
		        
		        for (int j = 0;j < nvars;++j){
		           if(j < colcount)
		           System.out.println("x[" + j +
		                              "]= " + x[j]);
		           else if(j != colcount + noOfBuses)
		        	   System.out.println("y[" + (j - colcount) +
	                              "]= " + x[j]);
		        }
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
	
	static void createLPModel(IloMPModeler model, double[]forecast, double[] sd, double[][] inverse, double[] pgiMax, double[] loadBus, double[] genBus, double[] Pli){
		
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
			finalPli[13] += 100 / bmva;
			
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
				
				lb[i] = 0;
				
				// Upper bound of selection variable set to 1
				
				ub[i] = 1;
				
				// Set type as integer
				
				type[i] = IloNumVarType.Int;
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
					constraintExpr.addTerm(inverse[i][(int)loadBus[j] - 1] * ((forecast[j] - sd[j]) / bmva), x[j]);				
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
			
			IloLinearNumExpr constraintExpr = model.linearNumExpr();		
			for(int i = 0;i < colcount;i++)
				constraintExpr.addTerm(1.0, x[i]);
			constraintExpr.addTerm(1.0, x[colcount + noOfBuses]);
			
			// RHS is selected + 1 since a decision variable corresponding to constant has been added
			
			row[2 * noOfBuses - 2] = model.addEq(constraintExpr, selected + 1);
			
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
				constraintExprSlack.addTerm(inverse[0][(int)loadBus[j] - 1] * ((forecast[j] - sd[j]) / bmva), x[j]);				
			}
			
			// Add constant term
			
			constraintExprSlack.addTerm(constant[0], x[colcount + noOfBuses]);
			
			// Slack bus phase angle is set to zero
			
			row[2 * noOfBuses] = model.addEq(constraintExprSlack, 0);
						
			// Create an expression object to store objective function
			
			IloLinearNumExpr objExpr = model.linearNumExpr();
			
			// Add terms corresponding to renewable generators
			
		    for (int i = 0; i < colcount; ++i){
		    	objExpr.addTerm(forecast[i] - sd[i], x[i]);
		    }
		    
		    // Maximize the objective function
		    
		    IloObjective obj = model.maximize(objExpr);		    
		    model.add(obj);
		    
		    System.out.println("demand = "+demand);
		    System.out.println("load at each bus");
		    for(int i = 0;i < noOfBuses;i++)
		    	System.out.println(finalPli[i] * bmva);
		    System.out.println("power forecast values\n");
			for(int i = 0;i < noOfLocations;i++)
				System.out.print(forecast[i] + "\t");
			System.out.println("power committed values\n");
			for(int i = 0;i < noOfLocations;i++)
				System.out.print(forecast[i] - sd[i] + "\t"); //assume k = 1
			
		}
		catch(Exception e){System.out.println("exxxxception occured" + e);}
		
	}
	
	
	/* Method findBusAdmittance() - calculate bus admittance matrix corresponding to IEEE 14 bus system 
	 * YbusImaginary - imaginary part of bus admittance matrix
	 * */
	
	static void findBusAdmittance(double[][] YbusImaginary){
		try{
						
			// Declare input stream for reading file
			
			FileInputStream loadfile = new FileInputStream(new File(gridPath));
			
			// Declare a workbook for opening .xls file
			
			HSSFWorkbook workbook = new HSSFWorkbook(loadfile);
			
			// Select sheet number 1 for reading
			
			HSSFSheet sheet = workbook.getSheetAt(1);
			
			// rcount - keeps track of the row number
			
			int rcount = -1;
			
			// Declare variables to store bus numbers and parameters like resistance, reactance etc.
			
			double[] fbus = new double[noOfBranches];
			double[] tbus = new double[noOfBranches];
			double[] resistance = new double[noOfBranches];
			double[] reactance = new double[noOfBranches];
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

}
