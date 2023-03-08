import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Title: CellularAutomatonMazeGenerator
 * Author: Yannis Seimenis
 * Description: Generates maze-like patterns on a grid following cellular automata Maze and Mazectric rulestrings.
 * Date: 08/03/2023
 */

public class Main {

    public static JFrame frame;
    public static JPanel[][] mazeGrid;

    public static Color aliveColor = Color.WHITE;//alive cell colour
    public static Color deadColor = Color.BLACK; //background color/dead cell colour

    public static int width = 800; //width of maze grid (pixels)
    public static int height = 600; //height of maze grid (pixels)
    public static int gridSize = 5; //height/width of individual cell (pixels) Must be divisor of width and height
    public static int timeout = 50; //timeout between generations (ms)
    public static int rule = 0; //rule 0 = B3/S1234, rule 1 = B3/S12345

    /**
     * main method
     * @param args
     */
    public static void main(String[] args) {
        initialiseFrame();
        initialiseMazeGrid();
        frame.setVisible(true);
        startGeneration();
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
        frame.setSize(width + 16, height + 39);
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
            mouseDown = true;
            drawCellOnGrid(e);
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
        } else if (aliveNeighbours > (4 + rule) || aliveNeighbours == 0) {
            mazeGrid[x][y].setBackground(deadColor);
        }
    }

    /**
     * Starts the cellular maze generation in new thread
     */
    public static void startGeneration() {
        Thread thread = new Thread(() -> {
            while (true) {
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
        thread.start();
    }

}
