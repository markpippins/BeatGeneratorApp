// package com.angrysurfer.midi.model.test;

// import static org.junit.jupiter.api.Assertions.assertEquals;

// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.stream.LongStream;

// import org.junit.jupiter.api.Test;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// import com.angrysurfer.core.model.Session;

// public class SessionTests {

  
//   static Logger logger = LoggerFactory.getLogger(SessionTests.class.getCanonicalName());

//     @Test
//     public void whenTime_thenBarChanges() {

//       AtomicInteger bar = new AtomicInteger(1);
//       Session session = new Session() {
        
//         public void onBarChange() {
//             super.onBarChange();
//             bar.incrementAndGet();
//         }
//       };

//       session.setBeatsPerBar(4);
//       session.setTicksPerBeat(4);
//       session.setPartLength(16);

//       int expectedBar = 2;

//       session.beforeStart();
      
//       // 
//       LongStream.range(0, session.getBeatsPerBar() * session.getTicksPerBeat()).forEach(i -> {
//         session.beforeTick();
//         session.afterTick();
//       });

//       // session.afterEnd();
//       assertEquals(expectedBar, bar.get());
//     }

//     @Test
//     public void whenTime_thenPartChanges() {

//       Session session = new Session();

//       session.setParts(4);
//       session.setBeatsPerBar(4);
//       session.setTicksPerBeat(1);
//       session.setPartLength(4);

//       int expectedPart = 1;

//       session.beforeStart();
//       for (int i = 0; i < 8; i++) {
//         session.beforeTick();
//         logger.info(String.format("%s", session.getPart()));
//         session.afterTick();
//       };

//     //   session.afterEnd();
//       assertEquals(expectedPart, session.getPart());
//     }

//     // @Test
//     // public void whenTime_thenPartCyclerChanges() {

//     //   Session session = new Session();

//     //   session.setBeatsPerBar(4);
//     //   session.setTicksPerBeat(4);
//     //   session.setPartLength(16);

//     //   int expectedPart = 2;

//     //   session.beforeStart();
      
//     //   // 
//     //   IntStream.range(0, 16 * session.getBeatsPerBar() * session.getTicksPerBeat()).forEach(i -> {
//     //     session.beforeTick();
//     //     session.afterTick();
//     //   });

//     // //   session.afterEnd();
//     //   assertEquals(expectedPart, session.getPartCycler().get());
//     // }
    
//     @Test
//     public void whenTime_thenPartCyclesBackToOne() {

//       Session session = new Session();

//       session.setBeatsPerBar(4);
//       session.setTicksPerBeat(4);
//       session.setPartLength(4);

//       int expectedPart = 1;
//       session.beforeStart();

//       LongStream.range(0, 16 * session.getBeatsPerBar() * session.getTicksPerBeat()).forEach(i -> {
//         session.beforeTick();
//         session.afterTick();
//       });

//       assertEquals(expectedPart, session.getPart());
//     }

// }
