# pulsar-otel-function

```shell 
docker compose down --volumes

# run the app container and pulsar broker in your local docker 
docker compose up
```


Simple function, intended to work with the pulsar-otel app/project

Swap in whatever function you want to test by updating:

1) the `--classname` value in `local-run.sh`
2) the `<mainClass>` value in `pom.xml`

```shell 
local-run.sh
```