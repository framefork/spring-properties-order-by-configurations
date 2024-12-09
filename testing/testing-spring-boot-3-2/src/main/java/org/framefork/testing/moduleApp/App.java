package org.framefork.testing.moduleApp;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(Configuration5.class)
public class App
{

}
