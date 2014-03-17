package test;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import client.Client;

public class TestNewServerSearchAlgorithm extends JPanel {


    private static JFrame display;
    private static final int GRID_WIDTH = 100;
    private static final int GRID_HEIGHT = 100;
    private static final int CELL_WIDTH = 5;
    private static final int CELL_HEIGHT = 5;
    private static final int CELL_EMPTY = 0;
    private static final int CELL_EXISTS = 1;
    private static final int CELL_VISITED = 2;
    private int [][]grid = new int[GRID_WIDTH][GRID_HEIGHT];
    
    
    //For the algorithm
    private int x = GRID_WIDTH / 2;
    private int y = GRID_HEIGHT / 2;
    private enum Direction {
    	NORTH, EAST, SOUTH, WEST;
    }
    Direction dir = Direction.EAST;
    
    public class NextStepActionListener implements ActionListener {
    	private TestNewServerSearchAlgorithm testPanel;
    	public NextStepActionListener(TestNewServerSearchAlgorithm testPanel) {
    		this.testPanel = testPanel;
    	}
    	
		@Override
		public void actionPerformed(ActionEvent arg0) {
			testPanel.nextStep();
		}
    	
    }
    
    public ActionListener getActionListener() {
    	return new NextStepActionListener(this);
    }
    
	public static void main(String[] args) {
    	display = new JFrame();
    	display.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	display.setSize((GRID_WIDTH + 1) * CELL_WIDTH, (GRID_HEIGHT + 1) * CELL_HEIGHT + 50);
    	
    	TestNewServerSearchAlgorithm testPanel = new TestNewServerSearchAlgorithm();
    	JPanel sharedPanel = new JPanel(new BorderLayout());
    	sharedPanel.add(testPanel, BorderLayout.CENTER);
    	JButton nextStepButton = new JButton("step");
    	nextStepButton.addActionListener(testPanel.getActionListener());
    	sharedPanel.add(nextStepButton, BorderLayout.NORTH);
    	display.add(sharedPanel);

    	display.setVisible(true);
    	
    	testPanel.init();
    	while (true) {
    		testPanel.repaint();
        	try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				Client.log.printerr(e);
			}
        }
	}

	@Override
	public void paintComponent(Graphics g) {
		for(int i = 0; i < GRID_WIDTH; ++i) {
			for(int j = 0; j < GRID_HEIGHT; ++j) {
				switch(grid[i][j]) {
				case CELL_VISITED:
					if(i == x && j == y) {
						g.setColor(Color.blue);
					} else {
						g.setColor(Color.green);
					}
					break;
				case CELL_EXISTS:
					g.setColor(Color.red);
					break;
				default:
					g.setColor(Color.black);
					break;
				}
				g.fillRect(i*CELL_WIDTH, j*CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);
				
				g.setColor(Color.black);
				g.drawRect(i*CELL_WIDTH, j*CELL_HEIGHT, CELL_WIDTH, CELL_HEIGHT);
			}
		}
	}
	
	public void nextStep() {
		/*
		if(x < GRID_WIDTH && y < GRID_HEIGHT) {
			grid[x++][y++] = CELL_VISITED;
		}
		*/
		
		System.out.println("Current: N=" + canStepNorth() + " E=" + canStepEast() + " S=" + canStepSouth() + " W=" + canStepWest());
		
		
		switch(dir) {
		case WEST:
			if(canStepWest()) {
				stepWest();
			} else if(canStepNorth()) {
				stepNorth();
			} else if(canStepEast()) {
				stepEast();
			} else if(canStepSouth()) {
				stepSouth();
			} else {
				System.out.println("Stuck!");
			}
			break;
		case SOUTH:
			if(canStepSouth()) {
				stepSouth();
			} else if(canStepWest()) {
				stepWest();
			} else if(canStepNorth()) {
				stepNorth();
			} else if(canStepEast()) {
				stepEast();
			} else {
				System.out.println("Stuck!");
			}
			break;
		case NORTH:
			if(canStepNorth()) {
				stepNorth();
			} else if(canStepEast()) {
				stepEast();
			} else if(canStepSouth()) {
				stepSouth();
			} else if(canStepWest()) {
				stepWest();
			} else {
				System.out.println("Stuck!");
			}
			break;
		default:	//EAST
			if(canStepEast()) {
				stepEast();
			} else if(canStepSouth()) {
				stepSouth();
			} else if(canStepWest()) {
				stepWest();
			} else if(canStepNorth()) {
				stepNorth();
			} else {
				System.out.println("Stuck!");
			}
			break;
		}
		grid[x][y] = CELL_VISITED;
	}
	
	private void stepNorth() {
		y--;
		dir = Direction.WEST;
		System.out.println("North clear, aim west");
	}
	
	private void stepEast() {
		x++;
		dir = Direction.NORTH;
		System.out.println("East clear, aim north");
	}
	
	private void stepSouth() {
		y++;
		dir = Direction.EAST;
		System.out.println("South clear, aim east");
	}
	
	private void stepWest() {
		x--;
		dir = Direction.SOUTH;
		System.out.println("West clear, aim south");
	}
	
	private boolean canStepNorth() {
		return grid[x][y-1] != CELL_EMPTY;
	}
	
	private boolean canStepEast() {
		return grid[x+1][y] != CELL_EMPTY;
	}
	
	private boolean canStepSouth() {
		return grid[x][y+1] != CELL_EMPTY;
	}
	
	private boolean canStepWest() {
		return grid[x-1][y] != CELL_EMPTY;
	}
	
	
	
	public void init() {
		grid[x][y] = CELL_EXISTS;
		Random rand = new Random();
		for(int i = 0; i < (GRID_WIDTH * GRID_HEIGHT); ++i) {
			int dir = rand.nextInt(4);
			switch(dir) {
			case 0:
				x++;
				if(x >= GRID_WIDTH) {
					x = GRID_WIDTH - 1;
				}
				break;
			case 1:
				x--;
				if(x < 0) {
					x = 0;
				}
				break;
			case 2:
				y++;
				if(y >= GRID_HEIGHT) {
					y = GRID_HEIGHT - 1;
				}
				break;
			default:
				y--;
				if(y < 0) {
					y = 0;
				}
				break;
			}
			
			grid[x][y] = CELL_EXISTS;
		}
		
		//Find a border we can start at
		x = GRID_WIDTH / 2;
		y = GRID_HEIGHT / 2;
		for(int j = GRID_HEIGHT / 2; j > 0; j--) {
			if(grid[x][j] == CELL_EXISTS && grid[x][j-1] == CELL_EMPTY) {
				y = j;
				System.out.println("Found at " + j);
				break;
			} else {
				System.out.println("At " + j  + ": " + grid[x][j] + ", " + grid[x][j-1]);
			}
		}
		grid[x][y] = CELL_VISITED;
	}
}
