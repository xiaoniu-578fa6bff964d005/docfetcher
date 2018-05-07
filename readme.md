# docfetcher

docfetcher with new features.

## Lucene6 Support and Chinese Analyzer.
Based on marvelous project [ansj_seg](https://github.com/NLPchina/ansj_seg), docfetcher provide new exprience for chinese user.

## type-ahead search
![Animated demonstration](/dev/screenshots/type-ahead-search.gif)

Users can toggle whether enable type-ahead search in preference window.


## docfetcher_py3
DocFetcher is an Open Source desktop search application. In order to maximize extensibility of docfetcher, this repository provide python access to docfetcher by integrating it with py4j.

One demo search command line tool which is written in Python can be found at [demo_search.py](/demo_search.py)

You don't have to reinstall the whole docfetcher to enjoy the python interface. Just copy `py4j0.10.7-py3.jar` and replace `net.sourceforge.docfetcher_1.1.19_xxxxxxxx-xxxx.jar` in `lib` directory in original docfetch release.

The python interface is only available when docfetcher main process rather than `docfetcher-daemon` is running.

### Warning

Using py4j without TLS can be dangerous because py4j runs a TCP server, by default on localhost, which is subject to both local and (potential) remote code execution exploits.

The py4j is disabled by default. In order to enable it manually, append code below to `conf/program-conf.txt`.

    OpenPy4jGatewayServer  = true
    Py4jPort=28834
