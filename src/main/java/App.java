import akka.actor.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class App {

    public static void main(String[] args) {
        ActorSystem actorSystem = ActorSystem.create("sample-superVisor");
        ActorRef actorRef = actorSystem.actorOf(Props.create(Supervisor.class));

        final CyclicBarrier gate = new CyclicBarrier(3);

        Thread t1 = new Thread(() -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Message message = new Message("message " + i);
                    actorRef.tell(message, ActorRef.noSender());
                }
                gate.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        });


        Thread t2 = new Thread(() -> {
            try {
                for (int i = 5; i < 10; i++) {
                    Message message = new Message("message " + i);
                    actorRef.tell(message, ActorRef.noSender());
                }
                gate.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        });

        t1.start();
        t2.start();
    }

}

class Supervisor extends UntypedActor {

    Queue<Message> messageList = new ArrayDeque<>();

    ActorSystem actorSystem = ActorSystem.create("sample-children");
    ActorRef actorRef = actorSystem.actorOf(Props.create(ChildActor.class));

    @Override
    public void onReceive(Object message) throws Throwable {
        if(message instanceof Message){
            messageList.add((Message) message);

            if(!CollectionUtils.isEmpty(messageList)){
                System.out.println("Queue is not empty.");
                for (Message m : messageList){
                    actorRef.tell(m, getSelf());
                    messageList.remove();
                }
            }else{
                System.out.println("Queue is empty.");
                actorRef.tell(message, getSelf());
            }
        }
    }

}


class ChildActor extends UntypedActor{

    @Override
    public void onReceive(Object message) throws Throwable {
        if(message instanceof Message){
            Thread.sleep(1000);
            System.out.println("Got message from parent : " + ((Message) message).getName());
        }
    }
}


@Data
@AllArgsConstructor
class Message{
    String name;
}