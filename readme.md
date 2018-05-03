# docfetcher_py3
DocFetcher is an Open Source desktop search application. In order to maximize extensibility of docfetcher, this repository provide python access to docfetcher by integrating it with py4j.

One demo search command line tool which is written in Python can be found at [demo_search.py](/demo_search.py)

You don't have to reinstall the whole docfetcher to enjoy the python interface. Just copy `py4j0.10.7-py3.jar` and replace `net.sourceforge.docfetcher_1.1.19_xxxxxxxx-xxxx.jar` in `lib` directory in original docfetch release.

The python interface is only available when docfetcher main process rather than `docfetcher-daemon` is running.
