# WINSOME
Il progetto consiste nella implementazione di WINSOME, un social media che si ispira a STEEMIT, una piattaforma social basata su blockchain la cui caratteristica più innovativa è quella di offrire una ricompensa agli utenti che pubblicano contenuti interessanti e a coloro (i curatori) che votano/commentano tali contenuti. La ricompensa viene data in Steem, una criptovaluta che può essere scambiata, tramite un Exchanger, con altre criptovalute come Bitcoin o con fiat currency. La gestione delle ricompense è effettuata mediante la blockchain Steem, su cui vengono registrati sia le ricompense che tutti i contenuti pubblicati dagli utenti. WINSOME, a differenza di STEEMIT, utilizza un’architettura client server in cui tutti i contenuti e le ricompense sono gestiti da un unico server piuttosto che da una blockchain.

## Note per la compilazione ed esecuzione
I seguenti comandi devono essere effettuati posizionandosi all’interno della cartella **src**. Per la
**compilazione** *lato client* utilizzare il comando:
```
javac WinsomeClient/api/*java WinsomeClient/impl/*java WinsomeClient/*java
```
mentre *lato server*:
```
javac -cp .:./WinsomeServer/lib/gson-2.8.9.jar WinsomeServer/api/*java WinsomeServer/impl/*java WinsomeServer/model/*java WinsomeServer/utils/*java WinsomeServer/*java
```
È allegato un makefile che automatizza la compilazione del client e server, semplicemente digitando make (con il target make clean si eliminano i file .class). Per effettuare l’**esecuzione** *lato client* utilizzare il comando:
```
java WinsomeClient.ClientMain
```
mentre *lato server*:
```
java -cp .:./WinsomeServer/lib/gson-2.8.9.jar WinsomeServer.ServerMain
```
Altrimenti si possono utilizzare i rispettivi jar:
```
java -jar Client.jar
java -jar Server.jar
```
