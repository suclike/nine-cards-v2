package com.fortysevendeg.ninecardslauncher.process.device.impl

import android.graphics.Bitmap
import com.fortysevendeg.ninecardslauncher.commons.NineCardExtensions._
import com.fortysevendeg.ninecardslauncher.commons.contexts.ContextSupport
import com.fortysevendeg.ninecardslauncher.commons.services.Service
import com.fortysevendeg.ninecardslauncher.commons.services.Service._
import com.fortysevendeg.ninecardslauncher.process.device._
import com.fortysevendeg.ninecardslauncher.process.utils.ApiUtils
import com.fortysevendeg.ninecardslauncher.services.api._
import com.fortysevendeg.ninecardslauncher.services.apps.AppsServices
import com.fortysevendeg.ninecardslauncher.services.contacts.models.{Contact => ServicesContact}
import com.fortysevendeg.ninecardslauncher.services.contacts.{ContactsServiceException, ContactsServices, ImplicitsContactsServiceExceptions}
import com.fortysevendeg.ninecardslauncher.services.image._
import com.fortysevendeg.ninecardslauncher.services.persistence._
import com.fortysevendeg.ninecardslauncher.services.persistence.models.{App, CacheCategory}
import com.fortysevendeg.ninecardslauncher.services.shortcuts.ShortcutsServices
import rapture.core.Answer

import scalaz.concurrent.Task

class DeviceProcessImpl(
  appsService: AppsServices,
  apiServices: ApiServices,
  persistenceServices: PersistenceServices,
  shortcutsServices: ShortcutsServices,
  contactsServices: ContactsServices,
  imageServices: ImageServices)
  extends DeviceProcess
  with ImplicitsDeviceException
  with ImplicitsImageExceptions
  with ImplicitsPersistenceServiceExceptions
  with ImplicitsContactsServiceExceptions
  with DeviceConversions {

  val apiUtils = new ApiUtils(persistenceServices)

  override def getSavedApps(implicit context: ContextSupport) =
    (for {
      apps <- persistenceServices.fetchApps
    } yield apps map toApp).resolve[AppException]

  override def saveInstalledApps(implicit context: ContextSupport) =
    (for {
      requestConfig <- apiUtils.getRequestConfig
      installedApps <- appsService.getInstalledApplications
      googlePlayPackagesResponse <- apiServices.googlePlayPackages(installedApps map (_.packageName))(requestConfig)
      appPaths <- createBitmapsFromAppPackage(toAppPackageSeq(installedApps))
      apps = installedApps map { app =>
        val path = appPaths.find { path =>
          path.packageName.equals(app.packageName) && path.className.equals(app.className)
        } map (_.path)
        val category = googlePlayPackagesResponse.packages.find { googlePlayPackage =>
          googlePlayPackage.app.docid.equals(app.packageName)
        } map (_.app.details.appDetails.appCategory.headOption.getOrElse(""))
        toAddAppRequest(app, category.getOrElse(""), path.getOrElse(""))
      }
      _ <- addApps(apps)
    } yield ()).resolve[AppException]

  override def saveApp(packageName: String)(implicit context: ContextSupport) =
    (for {
      requestConfig <- apiUtils.getRequestConfig
      app <- appsService.getApplication(packageName)
      googlePlayPackageResponse <- apiServices.googlePlayPackage(packageName)(requestConfig)
      appPackagePath <- imageServices.saveAppIcon(toAppPackage(app))
      _ <- persistenceServices.addApp(toAddAppRequest(app, googlePlayPackageResponse.app.details.appDetails.appCategory.headOption.getOrElse(""), appPackagePath.path))
    } yield ()).resolve[AppException]

  override def deleteApp(packageName: String)(implicit context: ContextSupport) =
    (for {
      _ <- persistenceServices.deleteAppByPackage(packageName)
    } yield ()).resolve[AppException]

  override def updateApp(packageName: String)(implicit context: ContextSupport) =
    (for {
      requestConfig <- apiUtils.getRequestConfig
      app <- appsService.getApplication(packageName)
      Some(appPersistence) <- persistenceServices.findAppByPackage(packageName)
      googlePlayPackageResponse <- apiServices.googlePlayPackage(packageName)(requestConfig)
      appPackagePath <- imageServices.saveAppIcon(toAppPackage(app))
      _ <- persistenceServices.updateApp(toUpdateAppRequest(appPersistence.id, app, googlePlayPackageResponse.app.details.appDetails.appCategory.headOption.getOrElse(""), appPackagePath.path))
    } yield ()).resolve[AppException]

  override def createBitmapsFromPackages(packages: Seq[String])(implicit context: ContextSupport) =
    (for {
      requestConfig <- apiUtils.getRequestConfig
      response <- apiServices.googlePlayPackages(packages)(requestConfig)
      _ <- createBitmapsFromAppWebSite(toAppWebSiteSeq(response.packages))
    } yield ()).resolve[CreateBitmapException]

  override def getAvailableShortcuts(implicit context: ContextSupport) =
    (for {
      shortcuts <- shortcutsServices.getShortcuts
    } yield toShortcutSeq(shortcuts)).resolve[ShortcutException]

  override def saveShortcutIcon(name: String, bitmap: Bitmap)(implicit context: ContextSupport) =
    (for {
      saveBitmapPath <- imageServices.saveBitmap(SaveBitmap(name, bitmap))
    } yield saveBitmapPath.path).resolve[ShortcutException]

  override def getFavoriteContacts(implicit context: ContextSupport) =
    (for {
      favoriteContacts <- contactsServices.getFavoriteContacts
      filledFavoriteContacts <- fillContacts(favoriteContacts)
    } yield toContactSeq(filledFavoriteContacts)).resolve[ContactException]

  override def getContacts(filter: ContactsFilter = AllContacts)(implicit context: ContextSupport) =
    (for {
      contacts <- filter match {
        case AllContacts => contactsServices.getContacts
        case FavoriteContacts => contactsServices.getFavoriteContacts
        case ContactsWithPhoneNumber => contactsServices.getContactsWithPhone
      }
    } yield toContactSeq(contacts)).resolve[ContactException]

  override def getContact(lookupKey: String)(implicit context: ContextSupport) =
    (for {
      contact <- contactsServices.findContactByLookupKey(lookupKey)
    } yield toContact(contact)).resolve[ContactException]

  private[this] def addApps(items: Seq[AddAppRequest]):
  ServiceDef2[Seq[App], PersistenceServiceException] = Service {
    val tasks = items map (persistenceServices.addApp(_).run)
    Task.gatherUnordered(tasks) map (list => CatchAll[PersistenceServiceException](list.collect { case Answer(app) => app }))
  }

  private[this] def addCacheCategories(items: Seq[AddCacheCategoryRequest]):
  ServiceDef2[Seq[CacheCategory], PersistenceServiceException] = Service {
    val tasks = items map (persistenceServices.addCacheCategory(_).run)
    Task.gatherUnordered(tasks) map (list => CatchAll[PersistenceServiceException](list.collect { case Answer(app) => app }))
  }

  private[this] def createBitmapsFromAppPackage(apps: Seq[AppPackage])(implicit context: ContextSupport):
  ServiceDef2[Seq[AppPackagePath], BitmapTransformationException] = Service {
    val tasks = apps map (imageServices.saveAppIcon(_).run)
    Task.gatherUnordered(tasks) map (list => CatchAll[BitmapTransformationException](list.collect { case Answer(app) => app }))
  }

  private[this] def createBitmapsFromAppWebSite(apps: Seq[AppWebsite])(implicit context: ContextSupport):
  ServiceDef2[Seq[AppWebsitePath], BitmapTransformationException] = Service {
    val tasks = apps map imageServices.saveAppIcon map (_.run)
    Task.gatherUnordered(tasks) map (list => CatchAll[BitmapTransformationException](list.collect { case Answer(app) => app }))
  }

  // TODO Change when ticket is finished (9C-235 - Fetch contacts from several lookup keys)
  private[this] def fillContacts(contacts: Seq[ServicesContact]) = Service {
    val tasks = contacts map (c => contactsServices.findContactByLookupKey(c.lookupKey).run)
    Task.gatherUnordered(tasks) map (list => CatchAll[ContactsServiceException](list.collect { case Answer(contact) => contact }))
  }.resolve[ContactException]

}
