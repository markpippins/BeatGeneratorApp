import "@typespec/http";
import "./core/sequencer/TimingUpdate.tsp";

using Http;
@service(#{ title: "Sequencer Service" })
namespace SequencerService;
