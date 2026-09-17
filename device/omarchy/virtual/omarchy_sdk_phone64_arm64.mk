# Copyright 2026 OmarchyOS
# SPDX-License-Identifier: Apache-2.0

# The stock SDK phone allocates 1.8 GiB to its dynamic partition group. The
# pinned on-device Codex and Claude Code runtimes are real native binaries, so
# reserve enough image space for them plus normal OTA growth without changing
# the partition layout used by physical-device ports.
BOARD_EMULATOR_DYNAMIC_PARTITIONS_SIZE := $(shell expr 3072 \* 1048576)

$(call inherit-product, device/generic/goldfish/64bitonly/product/sdk_phone64_arm64.mk)
$(call inherit-product, device/omarchy/virtual/omarchy_common.mk)

PRODUCT_NAME := omarchy_sdk_phone64_arm64
PRODUCT_MODEL := OmarchyOS Emulator Phone
