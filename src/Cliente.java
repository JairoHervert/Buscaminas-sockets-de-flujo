import java.io.*;
import java.net.Socket;

public class Cliente {
   public static void main(String[] args) {
      try {
         System.out.println("Ingresa el puerto del servidor: ");
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "ISO-8859-1"));
         String puerto = reader.readLine();
         System.out.println("Ingresa la dirección del servidor: ");
         String direccion = reader.readLine();
         Socket client = new Socket(direccion, Integer.parseInt(puerto));

         System.out.println("Cliente conectado al servidor...");

         PrintWriter dataOutput = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), "ISO-8859-1"));
         BufferedReader dataInput = new BufferedReader(new InputStreamReader(client.getInputStream(), "ISO-8859-1"));
         
         while (true) {
            try {
               System.out.println("Selecciona un nivel para el buscaminas: ");
               System.out.println("1. Fácil");
               System.out.println("2. Medio");
               System.out.println("3. Difícil");
               System.out.println("4. Salir");
               
               String inputDifficult = reader.readLine().trim(); // trim() elimina espacios en blanco al inicio y al final
               msgToServer(dataOutput, inputDifficult);
               
               System.out.println(dataInput.readLine() + "\n");   // Recibe mensaje de confirmación del servidor
               
               if (inputDifficult.isEmpty()) {  // Si el cliente envía una cadena vacía, se solicita de nuevo
                  continue;
               }
               
               int difficult = Integer.parseInt(inputDifficult);
               if (difficult == 4) {
                  // Cerrar conexiones
                  dataInput.close();
                  dataOutput.close();
                  client.close();
                  break;
               } else if (difficult < 1 || difficult > 4) {
                  continue;
               } else {
                  startGame(difficult, dataInput, dataOutput); // Iniciar juego
               }
               
            } catch (NumberFormatException e) {
               System.out.println("Entrada no válida. Por favor, ingresa un número.");
            } catch (IOException e) {
               e.printStackTrace();
               break; // Salir del bucle si ocurre un error de E/S
            }
         }

         
         
      }  catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   public static void startGame(int difficult, BufferedReader dataInput, PrintWriter dataOutput) {
      try {
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "ISO-8859-1"));
         String message = dataInput.readLine();
         System.out.println(message + "\n"); // recibe la respuesta del servidor (mensaje de inicio)
         
         char[][] board = null;
         switch (difficult) {
            case 1:
               board = createEmptyBoard(9, 9, 10);
               break;
            case 2:
               board = createEmptyBoard(16, 16, 40);
               break;
            case 3:
               board = createEmptyBoard(16, 30, 99);
               break;
         }
         
         while (true) {
            System.out.println("");
            seeBoard(board);
            System.out.println("Indica la acción que deseas realizar: ");
            System.out.println("1. Descubrir casilla");
            System.out.println("2. Marcar (o desmarcar) casilla");
            
            String action = reader.readLine();
            
            switch (action) {
               case "1":   // descubrir casilla
                  msgToServer(dataOutput, "1");
                  System.out.println("Entre a la funcion sel switch para descubrir");
                  openCell(board, dataInput, dataOutput);
                  break;
               case "2":   // marcar casilla
                  msgToServer(dataOutput, "2");
                  System.out.println("Entre a la funcion sel switch para flag");
                  setFlag(board, dataInput, dataOutput);
                  break;
               default:
                  System.out.println("Opción no válida. Por favor, intenta de nuevo.");
                  break;
            }

         }
         
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public static void seeBoard(char[][] board) {
      // Imprimir la fila superior con letras (A, B, C, ...)
      System.out.print("   "); // Espacio para alinear con los números de la izquierda
      for (int j = 0; j < board[0].length; j++) {
         if ('A' + j > 'Z') {
            System.out.print("\033[33m" + (char) ('A') + (j-25) + " \033[0m");
         } else {
            System.out.print("\033[33m" + (char) ('A' + j) + " \033[0m");
         }
      }
      System.out.println();
      
      // Imprimir el tablero con los números en la columna izquierda
      for (int i = 0; i < board.length; i++) {
         System.out.print("\033[33m" + (i + 1) + " \033[0m"); // Imprimir el número de la fila (1, 2, 3, etc.)
         if (i < 9) {
            System.out.print(" "); // Espacio para alinear con los números de la izquierda
         }
         for (int j = 0; j < board[i].length; j++) {
            if (board[i][j] == 'B') {
               System.out.print("\033[0;34m" + board[i][j] + "\033[0m");
            } else if (board[i][j] == '*') {
               System.out.print("\033[0;31m" + board[i][j] + "\033[0m");
            } else {
               System.out.print(board[i][j]);
            }

            if (j > 24) {
               System.out.print("  ");
            } else {
               System.out.print(" ");
            }
         }
         System.out.println(); // Salto de línea al final de cada fila
      }
   }
   
   public static char[][] createEmptyBoard(int rows, int cols, int mines) {
      char[][] board = new char[rows][cols];
      for (int i = 0; i < rows; i++) {
         for (int j = 0; j < cols; j++) {
            board[i][j] = '-';
         }
      }
      return board;
   }
   
   public static void openCell(char[][] board, BufferedReader dataInput, PrintWriter dataOutput) {
      try {
         while (true) {
            System.out.println("\nIndica la coordenada de la casilla que deseas descubrir (ejemplo: 2C): ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "ISO-8859-1"));
            
            String coordenades = reader.readLine();
            if (coordenades.isEmpty() || coordenades.length() < 2) {
               System.out.println("Coordenadas invalidas. Por favor, intenta de nuevo.");
               continue;
            }
            if (board[strToCoor(coordenades)[0]][strToCoor(coordenades)[1]] == 'B' ) {
               System.out.println("Casilla marcada. No se puede destapar.");
               continue;
            } else if (board[strToCoor(coordenades)[0]][strToCoor(coordenades)[1]] != '-') {
               System.out.println("Casilla ya destapada. Por favor, intenta de nuevo.");
               continue;
            }
            
            msgToServer(dataOutput, coordenades);
            
            String response = dataInput.readLine();
            
            if (response.equals("Coordenadas fuera de rango, intente de nuevo...")) {
               System.out.println(response);
               seeBoard(board);
               continue;
            } else if (response.equals("mina-found")) {
               String mines = dataInput.readLine();
               minesOnBoard(board, mines);
               System.out.println("\033[38;5;208m" + "¡Has perdido! Has encontrado una mina." + "\033[0m");
               seeBoard(board);
               System.exit(0);
               // solicitar nombre para guardar puntuación
               //endGame(dataInput, dataOutput);
               break;
            } else if (response.equals("empty-cell")) {
               System.out.println("Se ha destapado una casilla vacía.");
               String emptyCells = dataInput.readLine();
               String[] emptyCellsArray = emptyCells.split(",");
               
               for (String emptyCell : emptyCellsArray) {
                  emptyCell = emptyCell.trim();
                  if (board[Integer.parseInt(emptyCell.split(" ")[0])][Integer.parseInt(emptyCell.split(" ")[1])] == 'B') {
                     continue;
                  } else {
                     board[Integer.parseInt(emptyCell.split(" ")[0])][Integer.parseInt(emptyCell.split(" ")[1])] = '0';
                  }
               }
               break;
            } else if (response.equals("number-cell")) {
               String num = dataInput.readLine();
               String[] numAndRest = num.split("-");  // numAndRest[0] = número en la celda, numAndRest[1] = celdas restantes con numero
               
               board[strToCoor(coordenades)[0]][strToCoor(coordenades)[1]] = numAndRest[0].charAt(0);
               
               if (numAndRest[1].equals("zero")) {
                  System.out.println("\033[0;32mTodas las casillas con numeros han sido descubiertas, has ganado! \033[0m");
                  saveTime(dataInput, dataOutput);
                  System.exit(0);
                  break;
               }
               
               break;
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public static void minesOnBoard(char[][] board, String mines) {
      System.out.println("Entre a la funcion minesOnBoard");
      
      System.out.println("mines: " + mines);
      String[] minesArray = mines.split(",");
      
      for (String mine : minesArray) {
         mine = mine.trim();
         String[] coor = mine.split(" ");
         int row = Integer.parseInt(coor[0]);
         int col = Integer.parseInt(coor[1]);
         board[row][col] = '*';
      }
   }
   
   public static void setFlag(char[][] board, BufferedReader dataInput, PrintWriter dataOutput) {
      try {
         while (true) {
            System.out.println("\nIndica la coordenada de la casilla que deseas marcar o desmarcar (ejemplo: 2C): ");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "ISO-8859-1"));
            
            String coordenades = reader.readLine();
            if (coordenades.isEmpty() || coordenades.length() < 2) {
               System.out.println("Coordenadas invalidas. Por favor, intenta de nuevo.");
               continue;
            }
            
            // verificamos si en las coordenadas hay una 'B' si es asi se desmarca
            if (board[strToCoor(coordenades)[0]][strToCoor(coordenades)[1]] == 'B') {
               board[strToCoor(coordenades)[0]][strToCoor(coordenades)[1]] = '-';
               msgToServer(dataOutput, "dismark" + coordenades);
               System.out.println(dataInput.readLine());
               break;
            }
            
            if (board[strToCoor(coordenades)[0]][strToCoor(coordenades)[1]] != '-') {
               System.out.println("Casilla ya destapada. No se puede marcar.");
               continue;
            }
            
            msgToServer(dataOutput, coordenades);
   
            String response = dataInput.readLine();
            if (response.equals("Coordenadas fuera de rango, intente de nuevo...")) {
               System.out.println(response);
               seeBoard(board);
               continue;
            } else if (response.equals("marked-success")) {
               board[strToCoor(coordenades)[0]][strToCoor(coordenades)[1]] = 'B';
               break;
            } else if (response.equals("No tienes banderas disponibles...")) {
               System.out.println(response);
               break;
            } else { // Si se marcaron todas las casillas con minas
               System.out.println("\033[0;32m" + response + "\033[0m");
               saveTime(dataInput, dataOutput);
               System.exit(0);
               break;
            }
         }
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public static void saveTime(BufferedReader dataInput, PrintWriter dataOutput) {
      try {
         System.out.println("Ingresa tu nombre para guardar tu puntuación: ");
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, "ISO-8859-1"));
         String name = reader.readLine();
         msgToServer(dataOutput, name);
         
         // Recibir la puntuación del jugador a través del servidor
         String time = dataInput.readLine();
         System.out.println("\nTu tiempo fue de: \u001B[35m" + time + "\u001B[0m");
         
         // Recibir el ranking de los jugadores a través del servidor y mostrarlo
         System.out.println("Ranking de jugadores: \n");
         
         String line;
         while ((line = dataInput.readLine()) != null && !line.isEmpty()) {  // Leer cada línea del ranking
            System.out.println(line);  // Imprimir cada línea del ranking
         }
         
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   public static int[] strToCoor(String coordenades) {
      // podemos recibir coordenadas en formatos como: 2C, 2A, 2a, 2A1, 12A, 12a, 12A1, 12a1
      String row = "";
      String col = "";
      int rowInt = 0;
      int colInt = 0;

      while (Character.isDigit(coordenades.charAt(0))) {
         row += coordenades.charAt(0); // Concatenar el número de la fila (primeros dígitos)
         coordenades = coordenades.substring(1);   // restante de la cadena (columna)
      }
      
      if (coordenades.length() > 1) {  // si la columna es una letra y un número
         col = coordenades.charAt(0) + "";
         coordenades = coordenades.substring(1);
         colInt = 25 + Integer.parseInt(coordenades);
      } else { // si la columna es solo una letra
         colInt = Character.toUpperCase(coordenades.charAt(0)) - 'A';
      }
      rowInt = Integer.parseInt(row)-1;
      return new int[] {rowInt, colInt};
   }
   
   public static void msgToServer(PrintWriter dataOutput, String message) {
      dataOutput.println(message);
      dataOutput.flush();
   }
}
