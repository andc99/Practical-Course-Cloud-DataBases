package de.tum.i13.KVStoreTest;

import de.tum.i13.ECS.ECS;
import de.tum.i13.server.kv.KVCommandProcessor;
import de.tum.i13.server.kv.KVStore;
import de.tum.i13.server.nio.NioServer;
import de.tum.i13.shared.Config;
import de.tum.i13.shared.Constants;
import de.tum.i13.shared.InvalidPasswordException;
import de.tum.i13.shared.ServerStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @authors Yacouba Cisse, Luca Corbucci, Fabian Danisch
 */
public class testPassword {

    private static KVStore kv;
    private static Thread th;
    private static NioServer ns;

    private static Thread ecs;
    public static Integer port = 5186;


    private static void launchServer() {


        ECS ex = new ECS(52357, "OFF");
        ecs = new Thread(ex);
        ecs.start();
        while (!ex.isReady()) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        String[] args = {"-s", "FIFO", "-c", "100", "-d", "data/", "-l", "loggg.log", "-ll", "OFF", "-b", "127.0.0.1:52357", "-p", String.valueOf(port)};
        Config cfg = Config.parseCommandlineArgs(args);
        ServerStatus serverStatus = new ServerStatus(Constants.INACTIVE);

        kv = new KVStore(cfg, true, serverStatus);
        KVCommandProcessor cmdp = new KVCommandProcessor(kv);
        ns = new NioServer(cmdp);
        try {

            ns.bindSockets(cfg.listenaddr, cfg.port);

            th = new Thread(() -> {
                try {
                    ns.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            th.start();
        } catch (IOException e) {
        }
        while(!serverStatus.checkEqual(Constants.ACTIVE)){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @BeforeAll
    static void before() {
        launchServer();
    }


    @Test
    void testPutWithPassword() throws IOException {
        assertEquals(0, kv.put("Hello", "World"));
        assertEquals(0, kv.put("World", "Hello", "Pippo"));
    }

    @Test
    void testPutWithPassword2() throws IOException {
        assertEquals(0, kv.put("Ciao", "Mondo", "Pluto"));
    }

    @Test
    void testPutAndUpdateWithPassword() {
        assertEquals(0, kv.put("Ehi", "Mondo", "Pluto"));
        assertEquals(-3, kv.put("Ehi", "Ciao", "Pippo"));
        assertEquals(-3, kv.put("Ehi", "Ciao"));
        assertEquals(1, kv.put("Ehi", "AAAAA", "Pluto"));
    }

    @Test
    void testWithPasswordAndGet() {
        assertEquals(0, kv.put("AAAA", "Mondo", "Pluto"));
        assertEquals("invalid_password", kv.get("AAAA", "Pippo"));
        assertEquals("invalid_password", kv.get("AAAA"));
        assertEquals("Mondo", kv.get("AAAA", "Pluto"));
        assertEquals(-3, kv.put("AAAA", "Ciao", "Pippo"));
        assertEquals(-3, kv.put("AAAA", "Ciao"));
        assertEquals(1, kv.put("AAAA", "AAAAA", "Pluto"));
        assertEquals("invalid_password", kv.get("AAAA", "Pippo"));
        assertEquals("invalid_password", kv.get("AAAA"));
        assertEquals("AAAAA", kv.get("AAAA", "Pluto"));
    }

    @Test
    void testDeleteWithPassword() throws InvalidPasswordException {
        assertEquals(0, kv.put("ToDelete", "Mondo", "Pluto"));
        assertEquals("Mondo", kv.get("ToDelete", "Pluto"));
        Exception exception = assertThrows(InvalidPasswordException.class, () -> {
            kv.delete("ToDelete");
        });
        Exception exception2 = assertThrows(InvalidPasswordException.class, () -> {
            kv.delete("ToDelete", "ffdff");
        });
        assertEquals(1, kv.delete("ToDelete", "Pluto"));
    }


    @AfterAll
    static void afterAll() {
        Path path = Paths.get("data/");
        File[] files = new File(path.toAbsolutePath().toString() + "/").listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        ecs.interrupt();
        th.interrupt();
        ns.close();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
