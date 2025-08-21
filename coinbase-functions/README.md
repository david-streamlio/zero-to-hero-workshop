# coinbase-functions

## Deployment on BYOC

First you need to configure the context for the `pulsarctl` tool.

```bash
pulsarctl context set c-p1j162n \
    --admin-service-url https://pc-d7412b62.azure-usw3-production-7kcsh.test.azure.sn2.dev  \
    --issuer-endpoint https://auth.test.cloud.gcp.streamnative.dev/  \
    --audience urn:sn:pulsar:o-noc4n:flinkdemo \
    --key-file file:///Users/david/Downloads/o-noc4n-david-admin.json
    
pulsarctl oauth2 activate
```

Once you are authenticated, you can upload the artifacts

```bash
pulsarctl packages upload source://feeds/realtime/coinbase-source@v1 \
   --path /Users/david/clone-zone/personal/zero-to-hero-workshop/coinbase-functions/coinbase-live-feed/target/coinbase-live-feed-1.0.1.nar \
   --description "Coinbase live websocket API source" \
   --properties fileName=coinbase-live-feed-1.0.1.nar
   
pulsarctl packages upload function://feeds/realtime/feed-router@v1 \
  --path /Users/david/clone-zone/personal/zero-to-hero-workshop/coinbase-functions/coinbase-websocket-feed-router/target/coinbase-websocket-feed-router-1.0.1.nar \
  --description "Coinbase live feed router" \
  --properties fileName=coinbase-websocket-feed-router-1.0.1.nar
  
pulsarctl packages upload function://feeds/realtime/moving-average-flink-job@v1 \
  --path /Users/david/clone-zone/personal/zero-to-hero-workshop/coinbase-flink/trades-moving-average/target/trades-moving-average-0.0.1.jar \
  --description "Flink job to compute moving averages from coinbase ticker feed" \
  --properties fileName=trades-moving-average-0.0.1.jar
```

Then deploy

```bash
pulsarctl sources create \
  --source-config-file /Users/david/clone-zone/personal/zero-to-hero-workshop/coinbase-functions/coinbase-live-feed/src/main/resources/coinbase-source-config.yaml \
  --archive source://feeds/realtime/coinbase-source@v1
  
pulsarctl functions create \
  --function-config-file /Users/david/clone-zone/personal/zero-to-hero-workshop/coinbase-functions/coinbase-websocket-feed-router/src/main/resources/function-config.yaml \
  --jar function://feeds/realtime/feed-router@v1
```

```bash
snctl --organization o-noc4n context use
snctl apply -f /Users/david/clone-zone/personal/zero-to-hero-workshop/k8s/workspace.yaml

snctl apply -f /Users/david/clone-zone/personal/zero-to-hero-workshop/coinbase-flink/trades-moving-average/k8s/byoc-deployment.yaml
```

Enable access to the Ververica UI

```bash
# Authenticate against the StreamNative control plane in GCP
gcloud container clusters get-credentials api --region us-west1 --project sncloud-test

# Connect to the Node pool running the K8s cluster on Azure
apiserver-admin pools connect azure-usw3-production-7kcsh -n o-noc4n --context gke_sncloud-test_us-west1_api --org o-noc4n

# use kubectl to port forward the Ververica UI to a local port.
kubectl -n o-noc4n-vvp port-forward services/vvp-ververica-platform 8080:80
```

Teardown

```bash

snctl delete flinkdeployments -O o-noc4n o-noc4n-coinbase-moving-averages
```
