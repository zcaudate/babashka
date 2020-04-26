import org.graalvm.nativeimage.Platform;
import org.graalvm.nativeimage.Platforms;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.Delete;

public final class CutOffCoreServicesDependencies {
}

// @Platforms(Platform.DARWIN.class)
// @TargetClass(className = "sun.net.spi.DefaultProxySelector")
// @Delete
// final class Target_sun_net_spi_DefaultProxySelector {
// }

@Platforms(Platform.DARWIN.class)
@TargetClass(className = "apple.security.KeychainStore")
@Delete
final class Target_apple_security_KeychainStore {
}

@Delete
@TargetClass(className = "org.mariadb.jdbc.internal.com.send.authentication.SendPamAuthPacket")
final class Target_org_mariadb_jdbc_internal_com_send_authentication_SendPamAuthPacket {
}

// @Delete
// @TargetClass(className = "org.mariadb.jdbc.credential.aws.AwsCredentialGenerator")
// final class Target_org_mariadb_jdbc_credential_aws_AwsCredentialGenerator {
// }
