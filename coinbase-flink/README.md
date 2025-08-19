
## Deployment
To deploy this you need to do the following:

1. upload the fat jar file to Pulsar using `pulsarctl`, e.g., `pulsarctl packages upload function://feeds/realtime/jar-with-dependencies@demo --path <PATH TO JAR> --description <WHATEVER>
2. Create a workspace yaml file, e.g. workspace.yaml
3. Deploy the workspace using `snctl apply -f workspace.yaml`
4. Create a Flink Deployment yaml file, e.g. flink-deployment.yaml
5. Deploy the Flink job using `snctl apply -f flink-deployment.yaml`


### References

- https://www.youtube.com/watch?v=ILtUjkgK2oI