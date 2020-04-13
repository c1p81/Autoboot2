<?php
// se l'utente del demone webserver non puo' creare file nella directory 
// di lavoro si deve effettuare
// touch sensore.txt
// chmod 777 sensore.txt
// prima dell'avvio del sensore

// alcune variabili sono state messe per usi futuri

$azimuth=0.0;
$tempo = $_GET['tempo'];
$tempo = str_replace('_',' ',$tempo);
$pitch = $_GET['pitch'];
$pitch = str_replace(',','.',$pitch);
$roll = $_GET['roll'];
$roll = str_replace(',','.',$roll);
//$azimuth = $_GET['azimuth'];
$batteria = $_GET['batteria'];
$id_staz = $_GET['id_staz'];
$allarme = $_GET['allarme'];
$temp = $_GET['temp'];
$cicli = $_GET['cicli'];
$pressione = $_GET['pressione'];
$soglia = $_GET['soglia'];


$fp = fopen('sensore.txt', 'a');
fwrite($fp, $tempo.";".$pitch.";".$roll.";".$azimuth.";".$batteria.";".$id_staz.";".$allarme.";".$temp.";".$pressione.";".$cicli.";".$soglia.PHP_EOL);
fclose($fp);


// questo la risposta del web server in formato JSON
// serve a definire in modo dinamico al sensore le impostazioni
// per la frequenza di campionamento e la soglia in gradi di 
// attivazione dell'allarme istantaneo
print("{'cicli':20000,'soglia': 0.8}");
//phpinfo();
?>
