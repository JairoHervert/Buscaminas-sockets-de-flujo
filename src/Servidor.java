import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.StandardSocketOptions;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;

public class Servidor {
   public static void main(String[] args) {
      try {
         System.out.println("Ingresa el puerto del servidor: ");
         BufferedReader reader = new BufferedReader(new InputStreamReader(System.in,"ISO-8859-1"));
         String puerto = reader.readLine();
         
         // Crear el servidor en el puerto especificado y se reutiliza la dirección si el servidor se cierra
         ServerSocket server = new ServerSocket(Integer.parseInt(puerto));
         server.setOption(StandardSocketOptions.SO_REUSEADDR, true);
         
         System.out.println("Servicio iniciado en el puerto " + server.getLocalPort() + "  Esperando por clientes..");
         
         while (true) {
            Socket client = server.accept();
            System.out.println("Cliente conectado desde " + client.getInetAddress() + ":" + client.getPort() + "\n");
            
            PrintWriter dataOutput = new PrintWriter(new OutputStreamWriter(client.getOutputStream()));
            BufferedReader dataInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
            
            while (true) {
               String userDifficult = dataInput.readLine();
               if (userDifficult.isEmpty()) {  // Si el cliente envía una cadena vacía, se cierra la conexión
                  System.out.println("Entrada invalida, notificando al usuario...\n");
                  msgToClient(dataOutput, "Entrada invalida, intente de nuevo...");
                  continue;
               }
               
               int difficult = Integer.parseInt(userDifficult);
               if (difficult == 4) {
                  System.out.println("El cliente cerro la conexion" + "\n");
                  msgToClient(dataOutput, "Conexion finalizada...");
                  dataInput.close();
                  dataOutput.close();
                  client.close();
                  break;
               } else if (difficult < 1 || difficult > 4) {
                  System.out.println("Dificultad recibida no valida, notificando al usuario...\n");
                  msgToClient(dataOutput, "Dificultad no valida, intente de nuevo...");
               } else {
                  startGame(difficult, dataOutput, dataInput); // Iniciar juego
                  break;
               }
            }
         }
      } catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   public static void startGame(int difficult, PrintWriter dataOutput, BufferedReader dataInput) {
      // tablero, banderas permitidas, numero de celdas con numero, lista de coordenadas de minas
      char[][] board = null;
      int flags = 0;
      int cellsWithNumber = 0;
      List<Coordenada> minas = null;
      
      switch (difficult) {
         case 1:
            System.out.println("Iniciando juego, con la dificultad: Facil");
            msgToClient(dataOutput, "Iniciando juego, con la dificultad: Facil");
            Object[] resultFacil = createBoard(9, 9, 10);
            flags = 10;
            board = (char[][]) resultFacil[0];
            minas = (List<Coordenada>) resultFacil[1];
            cellsWithNumber = (int) resultFacil[2];
            break;
         case 2:
            System.out.println("Iniciando juego, con la dificultad: Medio");
            msgToClient(dataOutput, "Iniciando juego, con la dificultad: Medio");
            Object[] resultMedio = createBoard(16, 16, 40);
            flags = 40;
            board = (char[][]) resultMedio[0];
            minas = (List<Coordenada>) resultMedio[1];
            cellsWithNumber = (int) resultMedio[2];
            break;
         case 3:
            System.out.println("Iniciando juego, con la dificultad: Dificil");
            msgToClient(dataOutput, "Iniciando juego, con la dificultad: Dificil");
            Object[] resultDificil = createBoard(16, 30, 99);
            flags = 99;
            board = (char[][]) resultDificil[0];
            minas = (List<Coordenada>) resultDificil[1];
            cellsWithNumber = (int) resultDificil[2];
            break;
      }
      
      if (board != null) {
         dataOutput.println("El tablero ha sido creado exitosamente...");
         dataOutput.flush();
      }
   
      long startTime = System.currentTimeMillis(); // Guardar el tiempo de inicio del juego
      
      boolean mina = false;
      int flagsInMine = 0;
      while (!mina && flagsInMine < minas.size() && cellsWithNumber > 0) {
         System.out.print("\n");
         try {
            String action = dataInput.readLine();  // Leer la jugada del cliente (1 o 2)
            
            switch (action) {
               case "1":   // Descubrir casilla
                  while (true) {
                     System.out.println("Jugada de descubrir casilla");
                     String coorUser = dataInput.readLine();
                     int[] coor = strToCoor(coorUser);
                     
                     if (coor[0] < 0 || coor[0] >= board.length || coor[1] < 0 || coor[1] >= board[0].length) {
                        System.out.println("Coordenadas fuera de rango, notificando al usuario...");
                        msgToClient(dataOutput, "Coordenadas fuera de rango, intente de nuevo...");
                        break;
                     } else if (board[coor[0]][coor[1]] == '*') { // Mina encontrada, cliente pierde
                        System.out.println("\033[38;5;208m" + "Mina encontrada, el cliente ha perdido!" + "\033[0m");
                        msgToClient(dataOutput, "mina-found");
                        String minasInPairs = minesToPairs(minas);
                        msgToClient(dataOutput, minasInPairs);
                        mina = true;
                        break;
                     } else if (board[coor[0]][coor[1]] == '0') {
                        System.out.println("Casilla vacia, descubriendo casillas adyacentes...");
                        msgToClient(dataOutput, "empty-cell");
                        
                        // Descubrir celdas vacías adyacentes
                        String emptyCells = searchEmptyCells(board, coor[0], coor[1]);
                        msgToClient(dataOutput, emptyCells);
                        break;
                     } else { // Casilla con número descubierta
                        System.out.println("Casilla descubierta, contiene: " + board[coor[0]][coor[1]]);
                        cellsWithNumber--;
                        msgToClient(dataOutput, "number-cell");
                        
                        // Verificar si ha ganado descubriendo todas las casillas numéricas
                        if (cellsWithNumber == 0) {
                           System.out.println("\033[0;32m" + "Todas las casillas con numeros han sido descubiertas, el cliente gano!" + "\033[0m");
                           msgToClient(dataOutput, board[coor[0]][coor[1]] + "-zero");
                           
                           // recibir el nombre del cliente y guardar el tiempo
                           String nameRecived = dataInput.readLine();
                           saveTime(startTime, nameRecived, dataOutput);
                           
                           // Enviar el ranking al cliente y cerrar la conexión
                           sendRankingToClient(dataOutput);
                           closeConnection(dataOutput, dataInput);
                           return;
                        } else {
                           msgToClient(dataOutput, board[coor[0]][coor[1]] + "-notZero");
                        }
                        break;
                     }
                  }
                  break;
               
               case "2":   // Marcar casilla
                  while (true) {
                     System.out.println("Jugada de marcar casilla");
                     String coorUser = dataInput.readLine();
                     
                     // Manejar opciones segun la clave recibida
                     
                     if (coorUser.contains("dismark")) { // Desmarcar casilla
                        System.out.println("Desmarcando casilla...");
                        flags++;
                        
                        // Si la casilla desmarcada tiene una mina, restar de las minas marcadas
                        coorUser = coorUser.substring(7);
                        int[] coor = strToCoor(coorUser);
                        if (board[coor[0]][coor[1]] == '*') {
                           flagsInMine--;
                        }
                        
                        msgToClient(dataOutput, "Casilla desmarcada con exito...");
                        break;
                     }
                     
                     int[] coor = strToCoor(coorUser);
                     if (coor[0] < 0 || coor[0] >= board.length || coor[1] < 0 || coor[1] >= board[0].length) {
                        msgToClient(dataOutput, "Coordenadas fuera de rango, intente de nuevo...");
                        continue;
                     }
                     
                     // Marcar casilla
                     if (flags > 0) {
                        flags--;
                        if (board[coor[0]][coor[1]] == '*') {
                           flagsInMine++;
                        }
                        
                        // Verificar si ha ganado marcando todas las minas
                        if (flagsInMine == minas.size()) {
                           System.out.println("\033[0;32m" + "Todas las minas han sido marcadas con bandera, el cliente ha ganado!" + "\033[0m");
                           msgToClient(dataOutput, "Todas las minas han sido marcadas con bandera, has ganado!");
                           String nameRecived = dataInput.readLine();
                           saveTime(startTime, nameRecived, dataOutput);
                           
                           // Enviar el ranking al cliente y cerrar la conexión
                           sendRankingToClient(dataOutput);
                           closeConnection(dataOutput, dataInput);
                           return;
                        }
                        msgToClient(dataOutput, "marked-success");
                        break;
                     } else {
                        msgToClient(dataOutput, "No tienes banderas disponibles...");
                        break;
                     }
                  }
                  break;
               
               default:
                  break;
            }
         } catch (IOException e) {
            e.printStackTrace();
            break;
         }
      }
      
      // Si el cliente pierde (mina encontrada), cerrar la conexión
      if (mina) {
         closeConnection(dataOutput, dataInput);
      }
   }
   
   // Metodo para enviar el ranking al cliente
   public static void sendRankingToClient(PrintWriter dataOutput) throws IOException {
      // Leer el archivo de récords y enviarlo al cliente
      File file = new File("times.txt");
      if (file.exists()) {
         BufferedReader fileReader = new BufferedReader(new FileReader(file));
         String line;
         StringBuilder ranking = new StringBuilder();
         while ((line = fileReader.readLine()) != null) {
            ranking.append(line).append("\n");
         }
         fileReader.close();
         dataOutput.println(ranking.toString());
         dataOutput.flush();
      } else {
         dataOutput.println("No hay récords disponibles.");
         dataOutput.flush();
      }
   }
   
   // Metodo para cerrar la conexión con el cliente
   public static void closeConnection(PrintWriter dataOutput, BufferedReader dataInput) {
      try {
         dataInput.close();
         dataOutput.close();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }


   public static String minesToPairs(List<Coordenada> minas) {
      // pasar la lista de minas a un formato de pares: x y, x y, x y) ...
      String pairs = "";
      for (Coordenada mina : minas) {
         pairs += mina.x + " " + mina.y + ", ";
      }
      pairs = pairs.substring(0, pairs.length()-2); // Eliminar la última coma
      return pairs;
   }
   
   public static int minesAround(char[][] board, int x, int y) {
      int mines = 0;
      for (int i = x-1; i <= x+1; i++) {
         for (int j = y-1; j <= y+1; j++) {
            if (i >= 0 && i < board.length && j >= 0 && j < board[i].length) {
               if (board[i][j] == '*') {
                  mines++;
               }
            }
         }
      }
      return mines;
   }
   
   public static String searchEmptyCells(char[][] board, int x, int y) {
      // Usamos un set para almacenar las coordenadas visitadas (para evitar duplicados)
      Set<String> visited = new HashSet<>();
      StringBuilder emptyCells = new StringBuilder();
      
      // Llamamos a la función recursiva para encontrar todas las celdas vacías conectadas
      dfs(board, x, y, visited, emptyCells);
      // Eliminar la última coma y espacio
      emptyCells.delete(emptyCells.length()-2, emptyCells.length());

      return emptyCells.toString();
   }
   
   private static void dfs(char[][] board, int x, int y, Set<String> visited, StringBuilder emptyCells) {
      // Verificamos si las coordenadas están fuera de los límites del tablero
      if (x < 0 || x >= board.length || y < 0 || y >= board[0].length) {
         return;
      }
      
      // Convertimos la coordenada actual en un formato único para verificar si ya fue visitada
      String coord = x + " " + y;
      
      // Si ya visitamos esta celda o no es un '0', terminamos esta rama de la búsqueda
      if (visited.contains(coord) || board[x][y] != '0') {
         return;
      }
      
      // Marcamos esta celda como visitada
      visited.add(coord);
      // Añadimos esta coordenada a la cadena de resultado
      emptyCells.append(coord).append(", ");
      
      // Llamamos a la función recursivamente en las 8 direcciones (arriba, abajo, izquierda, derecha y diagonales)
      dfs(board, x - 1, y, visited, emptyCells);     // Arriba
      dfs(board, x + 1, y, visited, emptyCells);     // Abajo
      dfs(board, x, y - 1, visited, emptyCells);     // Izquierda
      dfs(board, x, y + 1, visited, emptyCells);     // Derecha
      dfs(board, x - 1, y - 1, visited, emptyCells); // Arriba-Izquierda
      dfs(board, x - 1, y + 1, visited, emptyCells); // Arriba-Derecha
      dfs(board, x + 1, y - 1, visited, emptyCells); // Abajo-Izquierda
      dfs(board, x + 1, y + 1, visited, emptyCells); // Abajo-Derecha
   }
   
   
   public static Object[] createBoard(int file, int column, int mines) {
      System.out.println("Creando tablero de " + file + "x" + column + " con " + mines + " minas");
      char[][] board = new char[file][column];
      List<Coordenada> minas = new ArrayList<>(); // Lista para almacenar coordenadas de minas
      
      // Colocar las minas aleatoriamente
      while (mines > 0) {
         int x = (int) (Math.random() * file);
         int y = (int) (Math.random() * column);
         if (board[x][y] != '*') {
            board[x][y] = '*';
            minas.add(new Coordenada(x, y));  // Guardar la coordenada de la mina
            mines--;
         }
      }
      
      // En las casillas que no son minas, contar cuantas minas hay alrededor y colocar el número
      int numberCells = 0;
      for (int i = 0; i < file; i++) {
         for (int j = 0; j < column; j++) {
            if (board[i][j] != '*') {
               int minesAround = minesAround(board, i, j);
               if (minesAround != 0) numberCells++;
               board[i][j] = (char) (minesAround + '0');
            }
         }
      }
      
      seeBoard(board);
      
      // Retornar el tablero y la lista de coordenadas de las minas
      return new Object[]{board, minas, numberCells};
   }
   
   public static void seeBoard(char[][] board) {
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
            if (j > 24) {
               System.out.print(board[i][j] + "  ");
            } else {
               System.out.print(board[i][j] + " ");
            }
         }
         System.out.println(); // Salto de línea al final de cada fila
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
   
   public static void msgToClient(PrintWriter dataOutput, String message) {
      dataOutput.println(message);
      dataOutput.flush();
   }
   
   public static void saveTime (long startTime, String name, PrintWriter dataOutput) throws IOException {
      long endTime = System.currentTimeMillis();
      long time = endTime - startTime;
      
      // Calcular el tiempo de juego en horas, minutos y segundos
      long hours = time / 3600000;
      long minutes = (time % 3600000) / 60000;
      long seconds = ((time % 3600000) % 60000) / 1000;
      
      String timeStr = hours + " hrs " + minutes + " minutos " + seconds + " segundos";
      System.out.println("Tiempo de juego: \u001B[35m" + timeStr + "\u001B[0m");
      dataOutput.println(timeStr);
      
      // verificar si el archivo de tiempos existe
      File file = new File("times.txt");
      if (!file.exists()) {
         try {
            file.createNewFile();
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      // el archivo solo contendra una lista de 20 tiempos record en el formato "1. nombre:  tiempo"
      try {
         BufferedReader fileReader = new BufferedReader(new FileReader(file));
         List<String> times = new ArrayList<>();
         String line;
         while ((line = fileReader.readLine()) != null) {
            times.add(line);
         }
         fileReader.close();
         

         // si la lista de tiempos esta vacia, se añade el tiempo actual
         if (times.isEmpty()) {
            // Si la lista de tiempos está vacía, se añade el tiempo actual
            times.add("1. " + name + ": " + hours + "h " + minutes + "m " + seconds + "s");
         } else {
            // Si la lista no está vacía, se verifica si el tiempo actual es mejor que algún récord guardado
            boolean added = false;
            for (int i = 0; i < times.size(); i++) {
               String[] parts = times.get(i).split(": ");
               String[] timeParts = parts[1].split(" ");
               
               int hoursRecord = Integer.parseInt(timeParts[0].substring(0, timeParts[0].length()-1));
               int minutesRecord = Integer.parseInt(timeParts[1].substring(0, timeParts[1].length()-1));
               int secondsRecord = Integer.parseInt(timeParts[2].substring(0, timeParts[2].length()-1));
               
               if (hours < hoursRecord || (hours == hoursRecord && minutes < minutesRecord) || (hours == hoursRecord && minutes == minutesRecord && seconds < secondsRecord)) {
                  times.add(i, (i + 1) + ". " + name + ": " + hours + "h " + minutes + "m " + seconds + "s");
                  added = true;
                  
                  // Actualizar los números de los tiempos que siguen al tiempo añadido

                  for (int j = i + 1; j < times.size(); j++) {
                     times.set(j, (j + 1) + ". " + times.get(j).split(". ", 2)[1]);
                  }
                  
                  break;
               }
            }
            
            // Si el nuevo tiempo no es mejor que ningún récord guardado, se añade al final
            if (!added) {
               times.add((times.size() + 1) + ". " + name + ": " + hours + "h " + minutes + "m " + seconds + "s");
            }
         }

         // Si la lista de tiempos tiene más de 20 elementos, se eliminan los últimos
         if (times.size() > 20) {
            times = times.subList(0, 20);
         }

         // Escribir la lista de tiempos actualizada en el archivo
         PrintWriter fileWriter = new PrintWriter(new FileWriter(file));
         for (String timeRecord : times) {
            fileWriter.println(timeRecord);
         }
         fileWriter.close();

         
      } catch (IOException e) {
         e.printStackTrace();
      }
      
   }
   
   static class Coordenada {
      int x, y;
      public Coordenada(int x, int y) {
         this.x = x;
         this.y = y;
      }
      @Override
      public String toString() {
         return "(" + x + ", " + y + ")";
      }
   }
}