#!/bin/bash
# Kubernetes deployment script for Coinbase data pipeline

set -e

NAMESPACE=${NAMESPACE:-coinbase-demo}
KUBECTL=${KUBECTL:-kubectl}

echo "=== Coinbase Data Pipeline K8s Deployment ==="

# Create namespace if it doesn't exist
echo "Creating namespace: $NAMESPACE"
$KUBECTL create namespace "$NAMESPACE" --dry-run=client -o yaml | $KUBECTL apply -f -

# Apply ConfigMap first
echo "Applying Pulsar configuration ConfigMap..."
$KUBECTL apply -f config/pulsar-configmap.yaml -n "$NAMESPACE"

# Apply Pulsar Functions and Sources
echo "Deploying Pulsar Functions and Sources..."
$KUBECTL apply -f deployment.yaml -n "$NAMESPACE"

# Apply Flink jobs (if using native K8s deployment)
echo "Deploying Flink jobs..."
$KUBECTL apply -f flink-jobs-deployment.yaml -n "$NAMESPACE"

# Wait for deployments to be ready
echo "Waiting for deployments to be ready..."
$KUBECTL wait --for=condition=available --timeout=300s deployment --all -n "$NAMESPACE"

# Show status
echo "=== Deployment Status ==="
$KUBECTL get all -n "$NAMESPACE"

echo ""
echo "=== ConfigMap Details ==="
$KUBECTL describe configmap pulsar-config -n "$NAMESPACE"

echo ""
echo "=== Deployment Complete ==="
echo "To check Flink job logs:"
echo "  kubectl logs -f deployment/flink-moving-average-job -n $NAMESPACE"
echo "  kubectl logs -f deployment/flink-aggregations-job -n $NAMESPACE"
echo ""
echo "To check Pulsar Function status:"
echo "  kubectl describe source coinbase-live-feed -n $NAMESPACE"
echo "  kubectl describe function coinbase-feed-router -n $NAMESPACE"
echo ""
echo "To update configuration:"
echo "  kubectl edit configmap pulsar-config -n $NAMESPACE"
echo "  kubectl rollout restart deployment -n $NAMESPACE"