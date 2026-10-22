import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
var encoder = new BCryptPasswordEncoder();
System.out.println("HASH=" + encoder.encode("Campus@2026"));
/exit
