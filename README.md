[![neow3j Actions Status](https://github.com/neow3j/neow3j/workflows/neow3j-ci-cd/badge.svg)](https://github.com/neow3j/neow3j/actions)
![Maven metadata URI](https://img.shields.io/maven-metadata/v/http/search.maven.org/maven2/io/neow3j/core/maven-metadata.xml.svg)
[![javadoc](https://javadoc.io/badge2/io.neow3j/core/javadoc.svg)](https://javadoc.io/doc/io.neow3j)
[![codecov](https://codecov.io/gh/neow3j/neow3j/branch/master-3.x/graph/badge.svg?token=Xd0m5I7cz0)](https://codecov.io/gh/neow3j/neow3j)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/f82a724b90a94df88e11c6462f2176ca)](https://www.codacy.com/manual/gsmachado/neow3j?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=neow3j/neow3j&amp;utm_campaign=Badge_Grade)

# neow3j: A Java/Kotlin/Android Development Toolkit for the Neo Blockchain

<p align="center">
<img src="./images/neow3j-neo3-with-balloon.png" alt="Bongo Cat Neow3j" width="400" height="291" />
</p>

Neow3j is a development toolkit that provides easy and reliable tools to build Neo dApps and Smart
Contracts using the Java platform (Java, Kotlin, Android).

The toolkit can be divided into the neow3j SDK, which is used for dApp development, and the
neow3j devpack, which is used for smart contract development. We use the term dApp development
for activities around the blockchain, e.g., building a web page that interacts with the
blockchain, but exclude the implementation of smart contracts. Of course, smart contracts will
naturally be part of a running dApp.

Neow3j is an open-source project developed by the community and maintained by
[AxLabs](https://axlabs.com).

Visit [neow3j.io](https://neow3j.io) for more information on neow3j and the technical documentation.

# Getting started

## SDK

To make use of all neow3j SDK features, add `io.neow3j:contract` project to your dependencies.
Neow3j is split into tow modules, so you can also depend on just the core functionality by adding
`io.neow3j.core` to your project.

__Gradle__

```groovy
implementation 'io.neow3j:contract:3.8.+'
```

__Maven__

```xml
<dependency>
    <groupId>io.neow3j</groupId>
    <artifactId>contract</artifactId>
    <version>[3.8.0,)</version>
</dependency>
```

Releases are available for Neo Legacy and Neo N3. The example above shows the newest release of neow3j for
Neo N3. To use the latest release for Neo Legacy, use the version `2.4.0`.

## Smart Contract Development

For smart contract development you require the `io.neow3j:devpack`. It provides all the Neo-related
utilities that are needed in a smart contracts. If you want to play around with the devpack add the
following dependency to your project.

__Gradle__

```groovy
implementation 'io.neow3j:devpack:3.8.+'
```

__Maven__

```xml
<dependency>
    <groupId>io.neow3j</groupId>
    <artifactId>compiler</artifactId>
    <version>[3.8.0,)</version>
</dependency>
```

> **Note:** The devpack and compiler are only available for Neo N3. Thus, Java cannot be used to
compile smart contracts that are compatible with Neo Legacy.

For help on how to compile a smart contract, check out the documentation about the neow3j compiler on [neow3j.io](https://neow3j.io/#/smart_contract_development/compilation?id=compilation).

## Donate :moneybag:

Help the development of neow3j by donating to the following addresses:

| Crypto   | Address                                      |
|----------|----------------------------------------------|
| NEO      | `AHb3PPUY6a36Gd6JXCkn8j8LKtbEUr3UfZ`         |
| ETH      | `0xe85EbabD96943655e2DcaC44d3F21DC75F403B2f` |
| BTC      | `3L4br7KQ8DCJEZ77nBjJfrukWEdVRXoKiy`         |


## Thanks and Credits :pray:

* [NEO Foundation](https://neo.org/contributors) & [NEO Global Development (NGD)](https://neo.org/contributors)
* This project was strongly based on [web3j](https://web3j.io),
a library originally developed by [Conor Svensson](http://conorsvensson.com), latest on [this commit](https://github.com/web3j/web3j/commit/2a259ece9736c0338fbb66b1be4c04aba0855254).
We are really thankful for it. :smiley:
