Erhöhung des Ordner-Überwachungs-Limits (Linux)
===============================================

Auf Linux können Prozesse höchstens 8192 Ordner gleichzeitig überwachen. Sie werden möglicherweise an dieses Limit stoßen, wenn Sie eine extrem tiefe Ordner-Hierarchie indizieren. Falls Ihnen das passiert, wird DocFetcher wahrscheinlich eine "No space left on device" Fehlermeldung anzeigen. Diese Feldermeldung wird verschwinden, sobald Sie das Ordner-Überwachungs-Limit erhöhen. Bspw. wird der folgende Terminal-Befehl das Limit auf 32000 erhöhen:

    sudo echo 32000 > /proc/sys/fs/inotify/max_user_watches

Um das Überwachungs-Limit permanent zu verändern, müssen Sie die Datei `/etc/sysctl.conf` mit Administrator-Rechten öffnen und folgende Zeile hinzufügen:

    fs.inotify.max_user_watches=32000
