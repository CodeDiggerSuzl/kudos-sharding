## kudos-sharing



### Dev Log

- [x] Code about dynamic datasource in spring 
- [x] Code about mybatis interceptor
- [x] Use ThreadLocal to connect db sharding result and table sharding result. 
- [x] Add config, ds number and name mapping map _2022/07/11_
- [x] Improve core sharding logic, make it ok.
    - [x] dynamic datasource is ok. _2022/07/21_
    - [x] interceptor is ok. _2022/07/21_
- [x] Get remote data group from config center,temp store in redis. _2022/07/31_
- [ ] Judge all type of parameter in mapper interface. half done. need to check.
- [ ] Decide sharding logic for specific cases.
- [ ] A table doesn't need to be sharded, but previous table was sharded.
- [ ] Druid pool config
- [ ] make the config much shorter.
- [ ] Use strategy pattern to choose sharding strategy
- [ ] Local cache to improve performance.
- [ ] Transaction support ? (not sure yet)
- [ ] Exception handling.
- [ ] Add instance cache make it much faster.
- [ ] Check code and validation of code
- [ ] Write test cases and other stuff
- [ ] Write docs.