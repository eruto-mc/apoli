> ## ⚠ これは改変版です（EdwinMindcraft/apoli の fork・公式のものではありません）
>
> **This is a modified version of Apoli (Forge port), not the official build.**
> Modified by the minecraft club (eruto). Original work: [EdwinMindcraft/apoli](https://github.com/EdwinMindcraft/apoli)
> (itself a port of [apace100/apoli](https://github.com/apace100/apoli)).
> Licensed under **MIT**, same as upstream. The original authors do not endorse this build.
>
> 上流: [EdwinMindcraft/apoli](https://github.com/EdwinMindcraft/apoli) ／ 枝 `eruto/world3-1.20.1`
> ／ 上流の枝 `1.20.x/forge` から分岐。**以下は上流の README です。**
> この枝は [eruto-mc/eruto-origins](https://github.com/eruto-mc/eruto-origins) の submodule として建てる（単体では建てない）。
>
> ⚠ **不具合をここの改変版で見つけても、上流へ報告しないでください。**
>
> **当部が変えたところ**（中身は `git log --author=erutobusiness`）:
>
> | 何を | なぜ |
> | - | - |
> | 描画まわりの mixin を `@Redirect` から MixinExtras の `@WrapOperation` へ書き換えた | `@Redirect` は 1 つの命令に 1 つしか当てられず、同じ所へ当てるほかの MOD が先に取っていると起動前に落ちる。`@WrapOperation` は重ねられる |
> | `ItemStack.copy` のあと、複製より先に元のスタックの capability を見るようにした | 作りたての複製に先に聞くと、コピー 1 回ごとに `AttachCapabilitiesEvent` がイベントバス全体へ飛んでいた（振る舞いは変わらない） |

# Apoli

[![JitPack](https://img.shields.io/jitpack/v/github/apace100/apoli?style=for-the-badge)](https://jitpack.io/#Apace100/apoli) [![Discord](https://img.shields.io/discord/734127708488859831?style=for-the-badge)](https://discord.gg/CnCBaRgwJD) ![GitHub issues](https://img.shields.io/github/issues/apace100/apoli?style=for-the-badge) ![GitHub pull requests](https://img.shields.io/github/issues-pr/apace100/apoli?style=for-the-badge) ![GitHub](https://img.shields.io/github/license/apace100/apoli?style=for-the-badge)

Apoli is a data-driven entity power provider. It allows you to attach powers to entities via datapacks.

## Maven
```groovy
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    modImplementation "com.github.apace100:apoli:[VERSION]"
}
```

## Documentation

Unfortunately Apoli doesn't have its own documentation yet, however you can reference the [documentation for Origins](https://origins.readthedocs.io/en/latest/) to see documentation from a consumer perspective. 

For additional support please use the [#addon-dev channel in the Origins discord](https://discord.gg/CnCBaRgwJD)
