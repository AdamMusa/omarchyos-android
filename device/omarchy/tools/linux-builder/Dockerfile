FROM ubuntu:24.04

ARG DEBIAN_FRONTEND=noninteractive
ARG BUILDER_UID=10001
ARG BUILDER_GID=10001

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        bc \
        bison \
        build-essential \
        ca-certificates \
        ccache \
        curl \
        file \
        flex \
        fontconfig \
        git-core \
        git-lfs \
        gnupg \
        lib32z1-dev \
        libc6-dev-i386 \
        libgl1-mesa-dev \
        libx11-dev \
        libxml2-utils \
        procps \
        repo \
        ripgrep \
        rsync \
        sudo \
        unzip \
        x11proto-core-dev \
        xsltproc \
        zip \
        zlib1g-dev \
    && rm -rf /var/lib/apt/lists/*

RUN groupadd --gid "${BUILDER_GID}" builder \
    && useradd --create-home --uid "${BUILDER_UID}" --gid "${BUILDER_GID}" --shell /bin/bash builder \
    && printf 'builder ALL=(ALL) NOPASSWD:ALL\n' >/etc/sudoers.d/builder \
    && chmod 0440 /etc/sudoers.d/builder

USER builder
WORKDIR /workspace

ENV USE_CCACHE=1
ENV CCACHE_EXEC=/usr/bin/ccache

CMD ["sleep", "infinity"]
