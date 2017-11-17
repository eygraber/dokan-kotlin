package com.dokany.kotlin

import com.dokany.kotlin.constants.MountError
import com.dokany.kotlin.structure.DeviceOptions
import com.dokany.kotlin.structure.DokanyFileInfo
import com.sun.jna.Native
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference

object NativeMethods {
    init {
        Native.register("dokan1")
    }

    /**
     * Mount a new Dokany Volume. This function block until the device is unmount. If the mount fail, it will directly return [MountError].
     *
     * @param options A [DeviceOptions] that describe the mount.
     * @param operations Instance of [DokanyOperations] that will be called for each request made by the kernel.
     * @return [MountError].
     */
    external fun DokanMain(options: DeviceOptions, operations: DokanyOperations): Int

    /**
     * Get the version of Dokan.
     *
     * @return The version number without the dots.
     */
    external fun DokanVersion(): Long

    /**
     * Get the version of the Dokany driver.
     *
     * @return The version number without the dots.
     */
    external fun DokanDriverVersion(): Long

    /**
     * Unmount a Dokany device from a driver letter.
     *
     * @param driveLetter Driver letter to unmount.
     * @return True if device was unmounted or false (in case of failure or device not found).
     */
    external fun DokanUnmount(driveLetter: Char): Boolean

    /**
     * Unmount a Dokany device from a mount point
     *
     * @param mountPoint Mount point to unmount
     *
     *  * Z
     *  * Z:
     *  * Z:\\
     *  * Z:\MyMountPoint
     *
     * @return if successfully unmounted mount point or not.
     */
    external fun DokanRemoveMountPoint(mountPoint: WString): Boolean

    /**
     * Extends the time out of the current IO operation in driver.
     *
     * @param timeout Extended time in milliseconds requested.
     * @param dokanyFileInfo [DokanyFileInfo] of the operation to extend.
     * @return if the operation was successful or not.
     */
    external fun DokanResetTimeout(timeout: Long, dokanyFileInfo: DokanyFileInfo): Boolean

    /**
     * Get the handle to Access Token.
     *
     * @param rawFileInfo [DokanyFileInfo] of the operation.
     * @return A handle to the account token for the user on whose behalf the code is running.
     */
    external fun DokanOpenRequestorToken(dokanyFileInfo: DokanyFileInfo): IntByReference

    /**
     * Convert [ZwCreateFile] parameters to CreateFile parameters.
     *
     * @param fileAttributes FileAttributes
     * @param createOptions CreateOptions
     * @param createDisposition CreateDisposition
     * @param outFileAttributesAndFlags
     * @param outCreationDisposition
     */
    external fun DokanMapKernelToUserCreateFileFlags(
            fileAttributes: Int,
            createOptions: Int,
            createDisposition: Int,
            outFileAttributesAndFlags: IntByReference,
            outCreationDisposition: IntByReference
    )

    /**
     * Convert IRP_MJ_CREATE DesiredAccess to generic rights.
     *
     * @param DesiredAccess Standard rights to convert
     * @return New DesiredAccess with generic rights.
     * @see {@linkplain https://msdn.microsoft.com/windows/hardware/drivers/ifs/access-mask}
     */
    // TODO: change return type and method parameter type to FileAccess
    external fun DokanMapStandardToGenericAccess(desiredAccess: Int): Int

    /**
     * Checks whether Name can match Expression.
     *
     * @param expression - Expression can contain wildcard characters (? and *)
     * @param name - Name to check
     * @param ignoreCase - Case sensitive or not
     * @return result if name matches the expression
     */
    external fun DokanIsNameInExpression(expression: String, name: String, ignoreCase: Boolean): Boolean

    /**
     *
     * @param serviceName
     * @param serviceType
     * @param serviceFullPath
     * @return
     */
    external fun DokanServiceInstall(serviceName: String, serviceType: Int, serviceFullPath: String): Boolean

    /**
     *
     * @param serviceName
     * @return
     */
    external fun DokanServiceDelete(serviceName: String): Boolean

    /**
     *
     * @return
     */
    external fun DokanNetworkProviderInstall(): Boolean

    /**
     *
     * @return
     */
    external fun DokanNetworkProviderUninstall(): Boolean

    /**
     * Determine if Dokany debug mode is enabled.
     *
     * @param mode
     * @return true if Dokany debug mode is enabled
     */
    external fun DokanSetDebugMode(mode: Int): Boolean

    /**
     * Enable or disable standard error output for Dokany.
     *
     * @param status
     */
    external fun DokanUseStdErr(status: Boolean)

    /**
     * Enable or disable Dokany debug mode.
     *
     * @param status
     */
    external fun DokanDebugMode(status: Boolean)

    /**
     * Get active Dokany mount points.
     *
     * @param list - Allocate array of DOKAN_CONTROL
     * @param length - Number of DOKAN_CONTROL instance in list.
     * @param uncOnly - Get only instances that have UNC Name.
     * @param nbRead- Number of instances successfully retrieved
     * @return List retrieved or not.
     */
    // TODO: Does this have proper params?
    external fun DokanGetMountPointList(fileAttributes: Long, length: Long, uncOnly: Boolean, nbRead: Long): Boolean

    /**
     * Convert Win32 error to NtStatus
     *
     * @see {@linkplain https://support.microsoft.com/en-us/kb/113996}
     *
     *
     * @param error - Win32 error to convert
     * @return NtStatus associated to the error
     */
    // TODO: Switch to NtStatus return type
    external fun DokanNtStatusFromWin32(error: Int): Int
}