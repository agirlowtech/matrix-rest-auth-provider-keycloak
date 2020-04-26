# Matrix Rest Auth Provider for Keycloak

> **Experimental** for now.  
> Project is being tested for using a Matrix Homeserver with Keycloak Authentication at [Agir Low-Tech](https://agir.lowtech.fr)

## Install

### Docker

#### Docker image with specific SBT and SCALA version

```
docker build \
  --build-arg BASE_IMAGE_TAG="8u212-b04-jdk-stretch" \
  --build-arg SBT_VERSION="1.3.10" \
  --build-arg SCALA_VERSION="2.13.1" \
  --build-arg USER_ID=1001 \
  --build-arg GROUP_ID=1001 \
  -t scala-sbt-rauthprovider \
  github.com/hseeberger/scala-sbt.git#:debian
```

#### Docker image containing our JAR

```
docker build -t rauthprovider --no-cache .
```

#### Optional : Test built images (easier for development)

```
docker run -it --rm --name rauthprovider --env-file ".env" -p "8003:8080" rauthprovider
```
  
### Keycloak

#### Create OpenID Client

1. Open your Realm at `https://YOUR.DOMAIN/auth/admin/REALM/console/#/realms/REALM`
2. Go to `Clients > Create`
3. Select `openid-connect`
4. Change `Access-Type` to `bearer-only`
5. Copy/Paste your `client_id` and `client_secret` into proper environment variables in your `.env` file (see `.env.template` for an example)  or directly in `docker-compose.yml`

### Configuration

- Edit your `.env` file accordingly 
- You can copy paste template with `cp .env.template .env`

## Usage

Run your container with `docker-compose up -d`

Stop with `docker-compose down`

## Resources

-  [Synapse REST Password provider](https://github.com/ma1uta/matrix-synapse-rest-password-provider) : allows to validate a password for a given username and return a user profile using an existing backend (Keycloak in our case, via OpenID)

## Credits

- [hseeberger/scala-sbt](https://github.com/hseeberger/scala-sbt)