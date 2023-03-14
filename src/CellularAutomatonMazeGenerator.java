import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Title: CellularAutomatonMazeGenerator
 * Author: Yannis Seimenis
 * Description: Generates maze-like patterns on a grid following cellular automata Maze and Mazectric rulestrings.
 * Date: 14/03/2023
 */

public class CellularAutomatonMazeGenerator {

    public static JFrame frame; //main frame
    public static JLabel statusLabel; //Status label
    public static JPanel[][] mazeGrid; //maze grid 2D array
    public static Thread generationThread; //maze generation thread
    public static Thread solveThread; //solve algorithm thread
    public static boolean generationRunning = false; //maze generation
    public static boolean selectSolve = false; //solve active
    public static boolean stopSolveOnEdge = false; //solve if hit edge
    public static Color aliveColor = Color.BLACK;//alive cell color
    public static Color deadColor = Color.WHITE; //background color/dead cell color
    public static Color solveColor = Color.RED; //color of solve pathway
    public static int width = 800; //width of maze grid (pixels)
    public static int height = 600; //height of maze grid (pixels)
    public static int gridSize = 5; //height/width of individual cell (pixels) Must be divisor of width and height
    public static int timeout = 50; //timeout between generations (ms)
    public static int generationRule = 0; //rule 0 = B3/S1234, rule 1 = B3/S12345

    /**
     * main method
     * @param args - Not currently used
     */
    public static void main(String[] args) {
        initialiseFrame();
        initialiseMazeGrid();
        initialiseMenuBar();
        frame.setVisible(true);
    }

    /**
     * Initialises JFrame
     */
    public static void initialiseFrame() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(null);
        frame.setResizable(false);
        frame.setTitle("Cellular Automaton Maze Generator");
        frame.setSize(width + 16, height + 62);
    }

    /**
     * Initialises JMenuBar
     */
    public static void initialiseMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);
        menuBar.add(fileMenu);

        JMenuItem exitMenuItem = new JMenuItem("Exit");
        fileMenu.add(exitMenuItem);

        JMenu generateMenu = new JMenu("Generate");
        generateMenu.setMnemonic(KeyEvent.VK_G);
        menuBar.add(generateMenu);

        JMenuItem startGenerationMenuItem = new JMenuItem("Start Maze Generation");
        generateMenu.add(startGenerationMenuItem);

        JMenuItem stopGenerationMenuItem = new JMenuItem("Stop Maze Generation");
        generateMenu.add(stopGenerationMenuItem);

        JMenuItem resetMazeGridMenuItem = new JMenuItem("Reset Maze Grid");
        generateMenu.add(resetMazeGridMenuItem);

        JMenuItem altRuleMenuItem = new JMenuItem("Use: B3/S12345 Rule");
        generateMenu.add(altRuleMenuItem);

        JMenuItem borderMenuItem = new JMenuItem("Insert Border");
        generateMenu.add(borderMenuItem);

        JMenu solveMenu = new JMenu("Solve");
        solveMenu.setMnemonic(KeyEvent.VK_S);
        menuBar.add(solveMenu);

        JMenuItem startSolveMenuItem = new JMenuItem("Start Solving");
        solveMenu.add(startSolveMenuItem);

        JMenuItem stopSolveMenuItem = new JMenuItem("Stop Solving");
        solveMenu.add(stopSolveMenuItem);

        JMenuItem resetSolveMenuItem = new JMenuItem("Reset Solve Paths");
        solveMenu.add(resetSolveMenuItem);

        JMenuItem stopSolveOnEdgeItem = new JMenuItem("Stop Solving if on Edge");
        solveMenu.add(stopSolveOnEdgeItem);

        menuBar.add(Box.createHorizontalGlue());

        statusLabel = new JLabel();
        statusLabel.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        menuBar.add(statusLabel);

        //Action Listeners
        exitMenuItem.addActionListener(e -> System.exit(0));

        startGenerationMenuItem.addActionListener(e -> {
            //Stop solve thread
            selectSolve = false;
            if (solveThread != null) {
                solveThread.interrupt();
            }
            generationRunning = true;
            startGeneration();
        });

        stopGenerationMenuItem.addActionListener(e -> {
            generationRunning = false;
            if (generationThread != null) {
                generationThread.interrupt();
            }
            updateStatus("Maze generation stopped");
        });

        resetMazeGridMenuItem.addActionListener(e -> {
            resetGrid();
        });

        altRuleMenuItem.addActionListener(e -> {
            if (generationRule == 0) {
                generationRule = 1;
                altRuleMenuItem.setText("Use: B3/S1234 Rule");
                updateStatus("Changed rule to B3/S12345");
            } else {
                generationRule = 0;
                altRuleMenuItem.setText("Use: B3/S12345 Rule");
                updateStatus("Changed rule to B3/S1234");
            }
        });

        borderMenuItem.addActionListener(e -> createBorder());

        startSolveMenuItem.addActionListener(e -> {
            //Stop main generation thread
            generationRunning = false;
            if (generationThread != null) {
                generationThread.interrupt();
            }
            updateStatus("Select a start point to solve from");
            selectSolve = true;
        });

        stopSolveMenuItem.addActionListener(e -> {
            updateStatus("Solving algorithm stopped");
            selectSolve = false;
            if (solveThread != null) {
                solveThread.interrupt();
            }
        });

        resetSolveMenuItem.addActionListener(e -> resetSolvePath());

        stopSolveOnEdgeItem.addActionListener(e -> {
            if (stopSolveOnEdge) {
                stopSolveOnEdge = false;
                stopSolveOnEdgeItem.setText("Stop Solving if on Edge");
                updateStatus("Algorithm will continue after hitting an edge");
            } else {
                stopSolveOnEdge = true;
                stopSolveOnEdgeItem.setText("Continue Solving if on Edge");
                updateStatus("Algorithm will terminate after hitting an edge");
            }
        });

    }

    /**
     * Updates status label in menu bar
     * @param status - New string for status bar
     */
    public static void updateStatus(String status) {
        statusLabel.setText(status + "  ");
    }

    /**
     * Initialises 2D array of JPanels for maze grid
     */
    public static void initialiseMazeGrid() {
        mazeGrid = new JPanel[width/gridSize][height/gridSize];
        int x = 0;
        int y = 0;
        for (int i = 0; i < width/gridSize; i++) {
            for (int j = 0; j < height/gridSize; j++) {
                mazeGrid[i][j] = new JPanel();
                mazeGrid[i][j].setBackground(deadColor);
                mazeGrid[i][j].setBounds(x, y, gridSize, gridSize);
                mazeGrid[i][j].setName(i + "," + j + "#");
                mazeGrid[i][j].addMouseListener(gridMouseListener);
                frame.add(mazeGrid[i][j]);
                y += gridSize;
            }
            x += gridSize;
            y = 0;
        }
    }

    /**
     * Mouse listener for drawing on maze grid
     */
    private static final MouseListener gridMouseListener = new MouseAdapter() {
        boolean mouseDown = false;
        @Override
        public void mousePressed(MouseEvent e) {
            if (selectSolve) {
                String s = e.getSource().toString();
                int x = Integer.parseInt(s.substring(s.indexOf("[") + 1, s.indexOf(",")));
                int y = Integer.parseInt(s.substring(s.indexOf(",") + 1, s.indexOf("#")));
                if (mazeGrid[x][y].getBackground() == deadColor) {
                    solve(x, y);
                }
            } else {
                mouseDown = true;
                drawCellOnGrid(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseDown = false;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (mouseDown) {
                drawCellOnGrid(e);
            }
        }
    };

    /**
     * Draws or removes cells from the maze grid
     * @param e MouseEvent sent from mouse listener
     */
    public static void drawCellOnGrid(MouseEvent e) {
        String s = e.getSource().toString();
        int x = Integer.parseInt(s.substring(s.indexOf("[") + 1, s.indexOf(",")));
        int y = Integer.parseInt(s.substring(s.indexOf(",") + 1, s.indexOf("#")));
        if (SwingUtilities.isRightMouseButton(e)) {
            mazeGrid[x][y].setBackground(deadColor);
        } else {
            mazeGrid[x][y].setBackground(aliveColor);
        }
    }

    /**
     * Checks for alive neighbours of a specified cell
     * @param x - x coordinate of cell to check
     * @param y - y coordinate of cell to check
     * @return - returns number of alive cells as an int
     */
    public static int checkNeighbours(int x, int y) {
        int aliveNeighbours = 0;
        //x - 1, y - 1
        if (x > 0 && y > 0) {
            if (mazeGrid[x - 1][y - 1].getBackground() == aliveColor) {
                aliveNeighbours++;
            }
        }
        //x, y - 1;
        if (y > 0) {
            if (mazeGrid[x][y - 1].getBackground() == aliveColor) {
                aliveNeighbours++;
            }
        }
        //x + 1, y - 1;
        if (x < width/gridSize - 1 && y > 0) {
            if (mazeGrid[x + 1][y - 1].getBackground() == aliveColor) {
                aliveNeighbours++;
            }
        }
        //x - 1, y;
        if (x > 0) {
            if (mazeGrid[x - 1][y].getBackground() == aliveColor) {
                aliveNeighbours++;
            }
        }
        //x + 1, y;
        if (x < width/gridSize - 1) {
            if (mazeGrid[x + 1][y].getBackground() == aliveColor) {
                aliveNeighbours++;
            }
        }
        //x -1, y + 1;
        if (x > 0 && y < height/gridSize - 1) {
            if (mazeGrid[x - 1][y + 1].getBackground() == aliveColor) {
                aliveNeighbours++;
            }
        }
        //x, y + 1;
        if (y < height/gridSize - 1) {
            if (mazeGrid[x][y + 1].getBackground() == aliveColor) {
                aliveNeighbours++;
            }
        }
        //x + 1, y + 1;
        if (x < width/gridSize - 1 && y < height/gridSize - 1) {
            if (mazeGrid[x + 1][y + 1].getBackground() == aliveColor) {
                aliveNeighbours++;
            }
        }
        return aliveNeighbours;
    }

    /**
     * Sets cell to alive or dead based on automation rules
     * @param x - x coordinate of cell to set
     * @param y - y coordinate of cell to check
     * @param aliveNeighbours - number of alive cells
     */
    public static void setNextCell(int x, int y, int aliveNeighbours) {
        if (aliveNeighbours == 3) {
            mazeGrid[x][y].setBackground(aliveColor);
        } else if (aliveNeighbours > (4 + generationRule) || aliveNeighbours == 0) {
            mazeGrid[x][y].setBackground(deadColor);
        }
    }

    /**
     * Starts the cellular maze generation in new thread
     */
    public static void startGeneration() {
        updateStatus("Generating maze pattern");
        generationThread = new Thread(() -> {
            while (generationRunning) {
                for (int i = 0; i < width/gridSize; i++) {
                    for (int j = 0; j < height/gridSize; j++) {
                        int aliveNeighbours = checkNeighbours(i, j);
                        setNextCell(i, j, aliveNeighbours);
                    }
                }
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ignored) {}
            }
        });
        generationThread.start();
    }

    /**
     * Resets the maze grid back to default
     */
    public static void resetGrid() {
        generationRunning = false;
        if (generationThread != null) {
            generationThread.interrupt();
        }
        for (int i = 0; i < width/gridSize; i++) {
            for (int j = 0; j < height/gridSize; j++) {
                mazeGrid[i][j].setBackground(deadColor);
            }
        }
        updateStatus("Maze grid reset");
    }

    /**
     * Attempts to solve/find pathway using random fill/intersection algorithm
     * @param x - X coordinate of starting point to solve from
     * @param y - Y coordinate of starting point to solve from
     */
    public static void solve(int x, int y) {
        updateStatus("Attempting to find maze solution");
        ArrayList<int[]> path = new ArrayList<>();
        ArrayList<int[]> intersections = new ArrayList<>();
        path.add(new int[] {x, y});
        //New thread for solve algorithm
        solveThread = new Thread(() -> {
            boolean terminated = false;
            int[] currentPoint = path.get(0);
            while (selectSolve) {
                int currentX = currentPoint[0];
                int currentY = currentPoint[1];
                mazeGrid[currentX][currentY].setBackground(solveColor);
                int neighbourPathCount = 0;
                ArrayList<int[]> nextPoints = new ArrayList<>();
                int[] nextPoint = new int[2];
                //Check surrounding cells for dead cells
                //above - x, y - 1
                if (currentY > 0) {
                    if (mazeGrid[currentX][currentY - 1].getBackground() == deadColor) {
                        neighbourPathCount++;
                        nextPoints.add(new int[] {currentX, currentY - 1});
                    }
                }
                //right - x + 1, y
                if (currentX < width/gridSize - 1) {
                    if (mazeGrid[currentX + 1][currentY].getBackground() == deadColor) {
                        neighbourPathCount++;
                        nextPoints.add(new int[] {currentX + 1, currentY});
                    }
                }
                //below - x, y + 1
                if (currentY < height/gridSize - 1) {
                    if (mazeGrid[currentX][currentY + 1].getBackground() == deadColor) {
                        neighbourPathCount++;
                        nextPoints.add(new int[] {currentX, currentY + 1});
                    }
                }
                //left - x -1, y
                if (currentX > 0) {
                    if (mazeGrid[currentX - 1][currentY].getBackground() == deadColor) {
                        neighbourPathCount++;
                        nextPoints.add(new int[] {currentX - 1, currentY});
                    }
                }
                //Decide next move based on neighbours
                //dead end
                if (neighbourPathCount == 0) {
                    for (int i = intersections.size() - 1; i >= 0; i--) {
                        if (isStillIntersection(intersections.get(i))) {
                            nextPoint = intersections.get(i);
                            break;
                        } else {
                            intersections.remove(i);
                            if (i == 0) {
                                //No more possible moves, terminate solution thread and loop
                                terminated = true;
                                selectSolve = false;
                                solveThread.interrupt();
                            }
                        }
                    }
                //one path
                } else if (neighbourPathCount == 1) {
                    nextPoint = nextPoints.get(0);
                //multiple path options
                } else {
                    //random path selection
                    intersections.add(currentPoint);
                    int randomSelection = ThreadLocalRandom.current().nextInt(0, neighbourPathCount);
                    nextPoint = nextPoints.get(randomSelection);
                }
                currentPoint = nextPoint;
                path.add(currentPoint);
                if (stopSolveOnEdge) {
                    if (currentPoint[0] == 0 || currentPoint[0] == width/gridSize - 1 || currentPoint[1] == 0 || currentPoint[1] == height/gridSize - 1) {
                        mazeGrid[currentPoint[0]][currentPoint[1]].setBackground(solveColor);
                        updateStatus("Possible maze pathway found!");
                        selectSolve = false;
                        solveThread.interrupt();
                    }
                }
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException ignored) {}
            }
            if (terminated) {
                updateStatus("No more pathways could be found!");
                selectSolve = true;
            }
        });
        solveThread.start();
    }

    /**
     * Checks if point has any available pathways to follow
     * @param point - 2D array containing points to check
     * @return - Returns boolean, true if point has available path/s
     */
    public static boolean isStillIntersection(int[] point) {
        int x = point[0];
        int y = point[1];
        //above - x, y - 1
        if (y > 0) {
            if (mazeGrid[x][y - 1].getBackground() == deadColor) {
                return true;
            }
        }
        //right - x + 1, y
        if (x < width/gridSize - 1) {
            if (mazeGrid[x + 1][y].getBackground() == deadColor) {
                return true;
            }
        }
        //below - x, y + 1
        if (y < height/gridSize - 1) {
            if (mazeGrid[x][y + 1].getBackground() == deadColor) {
                return true;
            }
        }
        //left - x -1, y
        if (x > 0) {
            if (mazeGrid[x - 1][y].getBackground() == deadColor) {
                return true;
            }
        }
        return false;
    }

    /**
     * Resets solve pathways back to default dead cell/background colour
     */
    public static void resetSolvePath() {
        updateStatus("Maze solution pathways reset");
        //Stop solve thread
        selectSolve = false;
        if (solveThread != null) {
            solveThread.interrupt();
        }
        for (int i = 0; i < width/gridSize; i++) {
            for (int j = 0; j < height/gridSize; j++) {
                if (mazeGrid[i][j].getBackground() == solveColor) {
                    mazeGrid[i][j].setBackground(deadColor);
                }
            }
        }
    }

    /**
     * Creates border around maze grid
     */
    public static void createBorder() {
        //Add borders to maze grid
        for (int i = 0; i < width/gridSize; i++) {
            mazeGrid[i][0].setBackground(aliveColor);
            mazeGrid[i][height/gridSize - 1].setBackground(aliveColor);
        }
        for (int i = 0; i < height/gridSize; i++) {
            mazeGrid[0][i].setBackground(aliveColor);
            mazeGrid[width/gridSize - 1][i].setBackground(aliveColor);
        }
        updateStatus("Inserted border!");
    }
    
}
