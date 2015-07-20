package com.fortysevendeg.ninecardslauncher.process.userconfig

import com.fortysevendeg.ninecardslauncher.process.userconfig.models.{UserCollectionItem, UserCollection, UserSimpleDevice, UserInfo}
import com.fortysevendeg.ninecardslauncher.services.api.models.{UserConfigCollectionItem, UserConfigCollection, UserConfigDevice, UserConfig}

trait UserConfigConversions {

  def toUserInfo(userConfig: UserConfig) = UserInfo(
    email = userConfig.email,
    name = userConfig.plusProfile.displayName,
    imageUrl = userConfig.plusProfile.profileImage.imageUrl,
    devices = userConfig.devices map toUserSimpleDevice
  )

  def toUserSimpleDevice(userConfigDevice: UserConfigDevice) = UserSimpleDevice(
    deviceId = userConfigDevice.deviceId,
    deviceName = userConfigDevice.deviceName
  )

  def toUserCollection(userConfigCollection: UserConfigCollection) = UserCollection(
    name = userConfigCollection.name,
    originalSharedCollectionId = userConfigCollection.originalSharedCollectionId,
    sharedCollectionId = userConfigCollection.sharedCollectionId,
    sharedCollectionSubscribed = userConfigCollection.sharedCollectionSubscribed,
    items = userConfigCollection.items map toUserCollectionItem,
    collectionType = userConfigCollection.collectionType,
    constrains = userConfigCollection.constrains,
    wifi = userConfigCollection.wifi,
    occurrence = userConfigCollection.occurrence,
    icon = userConfigCollection.icon,
    category = userConfigCollection.category
  )

  def toUserCollectionItem(item: UserConfigCollectionItem) = UserCollectionItem(
    itemType = item.itemType,
    title = item.title,
    intent = item.metadata.toString(),
    categories = item.categories
  )

}
