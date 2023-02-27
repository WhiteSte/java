FROM pdal/pdal:2.5.1
LABEL Grigory Pomadchin <daunnc@gmail.com>

ENV DEBIAN_FRONTEND noninteractive

RUN set -ex && \
    apt update -y && \
    apt install -y apt-transport-https gnupg

# Adoptium GPG keys and repository
RUN wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add -
RUN echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list

RUN set -ex && \
    apt update -y && \
    apt install --no-install-recommends -y temurin-11-jdk

ENV JAVA_HOME /usr/lib/jvm/temurin-11-jdk-amd64
RUN update-alternatives --set java `update-alternatives --list java | grep temurin-11`

RUN apt-get -y install bash gcc g++ cmake wget unzip gpg software-properties-common

RUN ln -s /opt/conda/envs/pdal/include/pdal /usr/include/pdal && \
    ln -s /usr/include /usr/lib/include && \
    ln -s /opt/conda/envs/pdal/lib /usr/lib/lib && \
    ln -s /opt/conda/envs/pdal/share/* /usr/share/* || true && \
    ln -s /opt/conda/envs/pdal/lib/* /usr/lib/* || true

# upd g++
RUN add-apt-repository -y ppa:ubuntu-toolchain-r/test
# strings /usr/lib/x86_64-linux-gnu/libstdc++.so.6 | grep GLIBCXX
RUN apt update -y && apt install -y g++-11
