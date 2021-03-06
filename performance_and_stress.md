# Roadmap del alumno

En principio la carga esperada para la aplicación es la de los alumnos respondiendo las encuestas. Así que le roadmap tiene que ser lo que nosotros esperamos que haga un alumno a la hora de llenar la encuesta.
Los pasos normales a seguir serían:

* POST /login
  * Autenticarse: obtener el token para poder realizar todas las demás operaciones
* GET /students/:filenumber
  * Obtener la información inicial para saber cuales son las encuestas abiertas.
* GET /careers/:career_short_name/polls/:poll_key
  * Obtener las preguntas e items de la poll a responder
* GET /students/:filenumber/careers/:career_short_name/polls/poll-result
  * Obtener la respuesta actual (404 en caso de que no haya respondido nunca)
* POST /students/:filenumber/careers/:career_short_name/polls/poll-result body: {} (caso de 404 en la request anterior)
  * Crear la respuesta
* PATCH /students/:filenumber/careers/:career_short_name/polls/poll-result
  * Enviar las respuestas
* POST /logout
  * Cerrar sesión

# Ambiente de pruebas

## Container de docker

* App
  * 2 cores
  * 2gb de memoria (0 de swap)

* Base de datos
  * 1 core
  * 2gb de memoria (0 de swap)
  
Ambos containers corrian en la misma computadora.

## Locust

El servidor de Locust que emulaba los alumnos (utilizando el roadmap mencionado) se ejecutaba en una computadora diferente a la que mantenia los containers.
  
## Red

Se destaca el hecho de que todas las pruebas se realizaron en una red domestica a traves de una conexion wifi. Consideramos que esto es relevante dado que en las graficas se puede observar diferencias entre el tiempo que la aplicacion tarda en generar la respuesta y el tiempo en que el usuario que realizo el pedido la recibe.  

# Performance tests

Como en nuestra aplicación se arman unas respuestas basadas en cada alumno en particular, a diferencia de las encuestas tradicionales nosotros asumimos que los alumnos responderán solo un subset de los items (ya que con suerte la mayoría ya estarán respondidas como ellos lo hubieran hecho). Una cosa que no sabemos en cuanto a la usabilidad es cuantas veces haran un update a sus antiguas respuestas, pero asumimos que será entre 0 y 4 veces como una suposición ya que no tenemos datos reales para hacer un análisis más inteligente.

En cuanto a cuántas materias responderá el alumno, elegimos una distribución gaussiana con μ=3, σ=1, basándonos en que la mayoría de los alumnos se anota en 3 materias por cuatrimestre, con varianza 1. Teniendo casos más raros de gente que se anota en 5 y gente que se anota en 1.

## Resultados

La siguiente imagen representa los resultados de 450 usuarios siguiendo el roadmap previamente explicado, durante aproximadamente 1 hora spawneando de a 50 cada 5 minutos. Como observaciones pudimos notar es que con 400 usuarios el response time es menor a 200ms, por lo que decidimos que 400 usuarios (~2.8k rpm) concurrentes es lo normal o performante para cada container. En el gráfico tambien se puede observar que con 450 usuarios el apdex se empieza a degradar notablemente (aunque se mantiene por encima del 0.9).

![performance_overview](https://user-images.githubusercontent.com/12850723/42144298-b6446b4c-7d90-11e8-8389-7e062ab447ff.png)

------------------------
Aca podemos ver como se comporta el heap asi como algunos datos mas de la memoria y el cpu:

![performance_heap1](https://user-images.githubusercontent.com/12850723/42144594-363ee2ae-7d92-11e8-9d29-f73af4ce104d.png)
![performance_heap2](https://user-images.githubusercontent.com/12850723/42144672-c1bd8510-7d92-11e8-84cd-70799e3e48f3.png)
![performance_heap3](https://user-images.githubusercontent.com/12850723/42144673-c4b2df7c-7d92-11e8-8872-9c14eea4a82a.png)

# Stress tests

La prueba consistió en aumentar la cantidad de usuarios de la siguiente forma en intervalos de aproximadamente 5 minutos:
 * 1 usuario
 * 50 usuarios
 * 100 usuarios
 * 250 usuarios
 * 500 usuarios
 * 1000 usuarios
 * 500 usuarios
 * 250 usuarios
 * 100 usuarios
 * 50 usuarios
 * 1 usuario

## Resultados

Una de las conclusiones que se pueden sacar de la primer imagen, es que el limite del container es ~4k rpm (1000 usuarios). Por otro lado se puede ver que la aplicación se comporta muy bien hasta los 500 usuarios (4:45 del eje x) que es en donde empiezan a subir los tiempos de respuesta un poco mas bruscamente.
![stress_overview](https://user-images.githubusercontent.com/12850723/42143651-a3e3dd06-7d8c-11e8-84d4-71e8f79ee561.png)

En la siguiente serie de imagenes se puede observar como el espacio eden del heap llega al máximo mientras que los demas espacios de memoria se mantienen en tamaños comparativamente pequeños.
![stress_heap1](https://user-images.githubusercontent.com/12850723/42143973-ccd61c9a-7d8e-11e8-9f33-8693cae0f5da.png)
![stress_heap2](https://user-images.githubusercontent.com/12850723/42143957-a2c937fc-7d8e-11e8-8c6a-0789840593c4.png)
![stress_heap3](https://user-images.githubusercontent.com/12850723/42143830-b1535be6-7d8d-11e8-99f4-b2a9cce77228.png)

Finalmente podemos observar como la memoria llega tambien muy cerca del limite del container
![stress_summary](https://user-images.githubusercontent.com/12850723/42144085-9220f326-7d8f-11e8-9439-79bb8a0ada7e.png)

# Conclusiones

Las conclusiones que se pueden sacar tras haber hecho estas pruebas son, en primer lugar, el hecho de que con un container relativamente chico se puede soportar una carga muy grande. Por otro lado vimos que esto nos permite escalar la aplicación horizontalmente de forma tal que con solo 2 containers en paralelo y un balanceador de carga podríamos soportar al rededor de ~800 usuarios o ~6000 rpm mientras la base de datos lo permita (gracias a que el servicio en si es stateless). Finalmente tambien podemos notar la diferencia de performance que notamos entre usar las configuraciones por defecto de play vs tunear el pool de play y de la base de datos, tunear la memoria, etc. Desde que empezamos a tunear las configuraciones hasta lo que quedo al final podemos decir que la aplicación pasó a bancarse al rededor de 5 veces más carga.

En cuanto a las pruebas de sterss estaría bueno destacar que sabemos que luego de pasar los 500 usuarios estaría bueno poner un "referí" que empiece a rechazar las conexiones para evitar sobrecargar la aplicación por cada container.
