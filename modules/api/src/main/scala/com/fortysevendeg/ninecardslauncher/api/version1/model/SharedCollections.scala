package com.fortysevendeg.ninecardslauncher.api.version1.model

case class ShareCollection(
  sharedCollectionId: Option[String],
  description: String,
  author: String,
  name: String,
  packages: Seq[String],
  category: String,
  icon: String,
  community: Boolean,
  tags: Seq[String] = Seq.empty, // TODO Remove this field in API 2.0
  occurrence: Seq[String] = Seq.empty, // TODO Remove this field in API 2.0,
  screenshots: Seq[String] = Seq.empty) // TODO Remove this field in API 2.0

case class SharedCollectionList(items: Seq[SharedCollection])

case class SharedCollection(
  _id: String,
  sharedCollectionId: String,
  publishedOn: Long,
  description: String,
  screenshots: Seq[AssetResponse],
  author: String,
  tags: Seq[String],
  name: String,
  shareLink: String,
  packages: Seq[String],
  resolvedPackages: Seq[SharedCollectionPackage],
  occurrence: Seq[UserConfigTimeSlot],
  lat: Double,
  lng: Double,
  alt: Double,
  views: Int,
  category: String,
  icon: String,
  community: Boolean)

case class AssetResponse(
  uri: String,
  title: String,
  description: String,
  contentType: String,
  thumbs: Seq[AssetThumbResponse])

case class AssetThumbResponse(
  url: String,
  width: Int,
  height: Int,
  `type`: String)

case class SharedCollectionPackage(
  packageName: String,
  title: String,
  description: String,
  icon: String,
  stars: Double,
  downloads: String,
  free: Boolean)