cd src

mvn eclipse:clean eclipse:eclipse clean install -Pwps,wps-cluster-hazelcast,wps-remote,importer,security,dyndimension,colormap,netcdf,netcdf-out,rest-ext,jms-cluster -DskipTests