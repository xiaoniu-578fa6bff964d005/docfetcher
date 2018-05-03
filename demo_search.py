# -*- coding: utf-8 -*-

def main():
    import sys
    if len(sys.argv)!=2:
        print("only accept one query.")
        return
    search(sys.argv[1])

def search(query):
    from py4j.java_gateway import JavaGateway, GatewayParameters
    from py4j.java_gateway import java_import

    from py4j.protocol import Py4JNetworkError
    try:
        gateway = JavaGateway(gateway_parameters=GatewayParameters(port=28834))
        java_import(gateway.jvm,'net.sourceforge.docfetcher.gui.Application')
        Application = gateway.jvm.net.sourceforge.docfetcher.gui.Application
        indexPanel=Application.indexPanel
        indexRegistry = indexPanel.getIndexRegistry()

        from py4j.protocol import Py4JJavaError
        try:
            searcher = indexRegistry.getSearcher()

            if searcher is None:
                return

            results=searcher.search(query)
        except Py4JJavaError as e:
            print(e)
            return

        for doc in results:
            print(doc.getFilename()+'\t'+doc.getPath().toString())
    except Py4JNetworkError as e:
        print("cannot connect to JVM.")

if __name__ == "__main__":
    main()
