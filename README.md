## Price publisher/consumer

### Description
2 interfaces were created `PriceConsumer` and `PricePublisher` together with implementations.  
I decided to separate interfaces because in this way we can control data flows more flexible.  
For example instead of saving prices batch we can create a message for later processing.  
Or instead of querying price directly we can request to create a file with all the prices.

#### Implemented data flow is the following:
- When batch run is announced unique batch run id is returned.
- After that price batches can be published with that batch run id.
- Batch run can be finished or canceled any time by the batch run id.
- When batch run is finished or canceled application removes batch run id and price can not be published for this run anymore.
- When batch run is finished the whole batch run data becoming accessible for querying.

#### Points for improvement
- Remove not finished batches by timeout.
- Do not copy price data immediately after finish. Event about finishing could be published to process data later.

### Running
`mvn clean test`