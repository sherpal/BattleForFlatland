# The menu server, with everything it serves to the players: the game server's jar and the Java
# runtimes they host games with (see game-hosting.md), and the packaged clients.
#
# Nothing is built here: build first (the runtimes and clients take a while, but the JDKs and LÖVE's
# builds they need are only downloaded once, in target/):
#
#   sbt packageApplication game-server/assembly game-server/gameServerRuntimes love-client/luaPackage
#   docker build -t battle-for-flatland .
#   docker run -p 9000:9000/tcp -p 9000:9000/udp battle-for-flatland
#
# On fly.io, `fly deploy` does the same docker build: the TCP port is the HTTP one, and the UDP one
# is the rendezvous, both being the PORT below (9000 by default).
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY dist/app.jar                    ./app.jar
COPY game-server/target/game-server.jar ./game-server.jar
COPY game-server/target/runtimes     ./runtimes
COPY love-client/target/dist         ./clients

# HTTP (TCP) and rendezvous (UDP, on the same port number unless RENDEZVOUS_PORT says otherwise)
ENV PORT=9000

EXPOSE 9000/tcp 9000/udp

# -DisProd=true makes the servers listen on all the interfaces (0.0.0.0) instead of the loopback only,
# without which nothing outside of the container can reach them.
# RENDEZVOUS_ADDRESS, when set, is the address that the rendezvous' UDP socket binds to (all the
# interfaces otherwise): on fly.io, `fly-global-services` (see fly.toml). It does not resolve elsewhere.
# The server's data (target/db) is relative to the working directory: it does not outlive the container.
CMD exec java \
    -XX:MaxRAMPercentage=75 \
    -DisProd=true \
    -Dport="$PORT" \
    -DrendezvousPort="${RENDEZVOUS_PORT:-$PORT}" \
    ${RENDEZVOUS_ADDRESS:+-DrendezvousAddress="$RENDEZVOUS_ADDRESS"} \
    -DgameServerJar=/app/game-server.jar \
    -DgameServerRuntimes=/app/runtimes \
    -DclientPackages=/app/clients \
    -jar /app/app.jar
