package com.fortysevendeg.ninecardslauncher.process.collection

import java.io.File

import com.fortysevendeg.ninecardslauncher.commons.services.Service.ServiceDef2
import com.fortysevendeg.ninecardslauncher.process.collection.models.NineCardIntentImplicits._
import com.fortysevendeg.ninecardslauncher.commons.contexts.ContextSupport
import com.fortysevendeg.ninecardslauncher.process.collection.models.{NineCardIntent, FormedItem, FormedCollection}
import com.fortysevendeg.ninecardslauncher.process.commons.CardType._
import com.fortysevendeg.ninecardslauncher.services.contacts.{ContactsServiceException, ContactsServices}
import com.fortysevendeg.ninecardslauncher.services.contacts.models.Contact
import com.fortysevendeg.ninecardslauncher.services.persistence.{AddCardRequest, AddCollectionRequest}
import com.fortysevendeg.ninecardslauncher.process.commons.Spaces._
import com.fortysevendeg.ninecardslauncher.services.utils.ResourceUtils
import play.api.libs.json.Json
import rapture.core.Answer

import scalaz.{\/-, -\/}

class FormedCollectionConversions(
  resourceUtils: ResourceUtils,
  contactsServices: ContactsServices) {

  def toAddCollectionRequest(formedCollections: Seq[FormedCollection])(implicit context: ContextSupport): Seq[AddCollectionRequest] =
    formedCollections.zipWithIndex.map(zipped => toAddCollectionRequest(zipped._1, zipped._2))

  def toAddCollectionRequest(formedCollection: FormedCollection, position: Int)(implicit context: ContextSupport) = AddCollectionRequest(
    position = position,
    name = formedCollection.name,
    collectionType = formedCollection.collectionType,
    icon = formedCollection.icon,
    themedColorIndex = position % numSpaces,
    appsCategory = formedCollection.category,
    constrains = None,
    originalSharedCollectionId = formedCollection.sharedCollectionId,
    sharedCollectionSubscribed = formedCollection.sharedCollectionSubscribed,
    sharedCollectionId = formedCollection.sharedCollectionId,
    cards = toAddCardRequest(formedCollection.items)
  )

  def toAddCardRequest(items: Seq[FormedItem])(implicit context: ContextSupport): Seq[AddCardRequest] =
    items.zipWithIndex.map(zipped => toAddCardRequest(zipped._1, zipped._2))

  def toAddCardRequest(item: FormedItem, position: Int)(implicit context: ContextSupport): AddCardRequest = {

    def fetchPhotoUri(
      extract: => Option[String],
      service: String => ServiceDef2[Option[Contact], ContactsServiceException]): Option[String] = {
      val maybeContact = extract flatMap { value =>
        val task = (for {
          s <- service(value)
        } yield s).run
        (task map {
          case Answer(r) => r
          case _ => None
        }).attemptRun match {
          case -\/(f) => None
          case \/-(f) => f
        }
      }
      maybeContact map (_.photoUri)
    }

    val nineCardIntent = jsonToNineCardIntent(item.intent)
    val path = (item.itemType match {
      case `app` =>
        for {
          packageName <- nineCardIntent.extractPackageName()
          className <- nineCardIntent.extractClassName()
        } yield {
          val pathWithClassName = resourceUtils.getPathPackage(packageName, className)
          // If the path using ClassName don't exist, we use a path using only packagename
          if (new File(pathWithClassName).exists) pathWithClassName else  resourceUtils.getPath(packageName)
        }
      case `phone` | `sms` =>
        fetchPhotoUri(nineCardIntent.extractPhone(), contactsServices.fetchContactByPhoneNumber)
      case `email` =>
        fetchPhotoUri(nineCardIntent.extractEmail(), contactsServices.fetchContactByEmail)
      case _ => None
    }) getOrElse "" // UI will create the default image
    AddCardRequest(
      position = position,
      term = item.title,
      packageName = nineCardIntent.extractPackageName(),
      cardType = item.itemType,
      intent = item.intent,
      imagePath = path
    )
  }

  private[this] def jsonToNineCardIntent(json: String) = Json.parse(json).as[NineCardIntent]

}
