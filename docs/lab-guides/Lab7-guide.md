# Hands-On Lab #7: Aggregations with Flink SQL

Prerequisites
------------

- [Lab1](Lab1-guide.md)
- [Lab2](Lab2-guide.md)
- [Lab3](Lab3-guide.md)
- [Lab4](Lab4-guide.md)
- [Lab5](Lab5-guide.md)
- [Lab6](Lab6-guide.md)


Flink Aggregations
--------------------------------------


### 1️⃣ Create the aggregation views

[05-create-product-aggregates-view.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Faggregations%2F05-create-product-aggregates-view.ddl)
[06-create-stats-aggregates-view.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Faggregations%2F06-create-stats-aggregates-view.ddl)


### Verification

`show tables;`

Should show the views in the list

### Ad-Hoc queries against the views

Lagging Windows
-------------------------------------

The LAG function in Apache Flink is a built-in window function that allows users to access data from a previous row in 
the same result set. It's useful for comparing values in the current row with values in a previous row, such as to 
analyze differences between consecutive rows or identify trends. For example, the LAG function can be used to calculate 
how much a stock price has increased or decreased over time.

- [07-create-price-changes-view.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Fmonitoring%2F07-create-price-changes-view.ddl)
- [08-create-price-spikes-view.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Fmonitoring%2F08-create-price-spikes-view.ddl)
- [09-create-volume-surges-view.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Fmonitoring%2F09-create-volume-surges-view.ddl)


Tumbling Windows
--------------------------------------

[Tumbling windows](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/operators/windows/#tumbling-windows) have a fixed size and do not overlap. or example, if you specify a tumbling window with a size of 5 
minutes, the current window will be evaluated and a new window will be started every five minutes.

![tumbling-windows.svg](..%2Fimages%2Ftumbling-windows.svg)

- [10-create-avg-price-view.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Fwindowing%2F09-create-avg-price-view.ddl)

Sliding Windows
--------------------------------------

With [sliding windows](https://nightlies.apache.org/flink/flink-docs-release-1.19/docs/dev/datastream/operators/windows/#sliding-windows), 
the size of the windows is configured by the window size parameter. An additional window slide parameter controls how 
frequently a sliding window is started. Hence, sliding windows can be overlapping if the slide is smaller than the window
size. In this case elements are assigned to multiple windows.

![sliding-windows.svg](..%2Fimages%2Fsliding-windows.svg)

For example, you could have windows of size 10 minutes that slides by 5 minutes. With this you get every 5 minutes a 
window that contains the events that arrived during the last 10 minutes.


- [11-create-sliding-window-avg-price-view.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Fwindowing%2F11-create-sliding-window-avg-price-view.ddl)
- [12-create-sliding-window-event-count-view.ddl](..%2F..%2Finfrastructure%2Fflink%2Fddls%2Fwindowing%2F10-create-sliding-window-event-count-view.ddl)




