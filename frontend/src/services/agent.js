import bookChunks from '../assets/book_chunks.json';

const DEFAULT_HUMAN_MEMORY = {
  age: 53,
  insomnia_duration: "Unknown",
  symptoms: "Insomnia",
  medications: "Unknown",
  sleep_goals: "Improve sleep quality, reduce nighttime awakenings, establish consistency",
  lifestyle: {
    exercise: "Unknown",
    caffeine: "Unknown",
    alcohol: "Unknown"
  },
  cbt_progress: {
    current_week: -1,
    current_week_description: "Initial Assessment Interview",
    sleep_window: "Not set",
    average_sleep_duration: 0,
    average_sleep_efficiency: 0.0
  }
};

const DEFAULT_PERSONA_MEMORY = {
  name: "Sleep Advisor",
  role: "CBT-I Specialist",
  style: "deep, caring, confident, structured, encouraging, empathetic",
  rules: [
    "Guide the user through the 6-week CBT-I program based on Gregg Jacobs' book.",
    "Ensure the user maintains a consistent wake-up time.",
    "Implement and enforce sleep restriction (time-in-bed limit) starting from Week 2.",
    "Help the user identify and challenge negative sleep thoughts (NSTs).",
    "Frequently reference relevant concepts from the book like 'core sleep', '8-hour myth', 'relaxation response'.",
    "Actively use memory tools (update_human_memory, insert_archival_memory) to persist important details.",
    "Keep responses highly engaging, concise, and conversational for voice playback."
  ]
};

// Initialize default local storage values
export function initializeStorage() {
  if (!localStorage.getItem("deepseek_api_key") && window.DEEPSEEK_API_KEY) {
    localStorage.setItem("deepseek_api_key", window.DEEPSEEK_API_KEY);
  }
  if (!localStorage.getItem("core_memory_human")) {
    localStorage.setItem("core_memory_human", JSON.stringify(DEFAULT_HUMAN_MEMORY, null, 2));
  }
  if (!localStorage.getItem("core_memory_persona")) {
    localStorage.setItem("core_memory_persona", JSON.stringify(DEFAULT_PERSONA_MEMORY, null, 2));
  }
  if (!localStorage.getItem("archival_memories")) {
    localStorage.setItem("archival_memories", JSON.stringify([]));
  }
  if (!localStorage.getItem("sleep_diaries")) {
    localStorage.setItem("sleep_diaries", JSON.stringify([]));
  }
  if (!localStorage.getItem("chat_history")) {
    localStorage.setItem("chat_history", JSON.stringify([]));
  }
  if (!localStorage.getItem("cbt_week")) {
    localStorage.setItem("cbt_week", "-1");
  }
}

// Client-side RAG search for the book
export function searchBook(query, limit = 4) {
  if (!bookChunks || bookChunks.length === 0) return [];
  
  const words = query.toLowerCase().split(/\s+/).filter(w => w.length > 2);
  if (words.length === 0) {
    return bookChunks.filter(chunk => chunk.toLowerCase().includes(query.toLowerCase())).slice(0, limit);
  }
  
  const scored = bookChunks.map((chunk, index) => {
    const chunkLower = chunk.toLowerCase();
    let score = 0;
    
    words.forEach(word => {
      if (chunkLower.includes(word)) {
        score += 10;
        const occurrences = chunkLower.split(word).length - 1;
        score += Math.min(occurrences, 5);
      }
    });
    
    if (chunkLower.includes(query.toLowerCase())) {
      score += 50;
    }
    
    return { chunk, score, index };
  });
  
  return scored
    .filter(item => item.score > 0)
    .sort((a, b) => b.score - a.score || a.index - b.index)
    .slice(0, limit)
    .map(item => item.chunk);
}

// Helper to calculate statistics
export function calculateSleepStats() {
  const diaries = JSON.parse(localStorage.getItem("sleep_diaries") || "[]");
  if (diaries.length === 0) {
    return {
      total_logs: 0,
      average_duration_mins: 0,
      average_efficiency: 0.0,
      average_latency_mins: 0,
      average_awakenings: 0.0,
      average_quality: 0.0,
      average_alertness: 0.0
    };
  }
  
  const timeDiffMins = (t1, t2) => {
    try {
      const [h1, m1] = t1.split(":").map(Number);
      const [h2, m2] = t2.split(":").map(Number);
      let mins = (h2 * 60 + m2) - (h1 * 60 + m1);
      if (mins < 0) mins += 24 * 60; // crossed midnight
      return mins;
    } catch {
      return 0;
    }
  };

  let totalDuration = 0;
  let totalEfficiency = 0.0;
  let totalLatency = 0;
  let totalAwakenings = 0;
  let totalQuality = 0.0;
  let totalAlertness = 0.0;
  let validDiaries = 0;
  
  diaries.forEach(d => {
    const tib = timeDiffMins(d.light_out_time || d.bed_time, d.out_of_bed_time);
    if (tib <= 0) return;
    
    const latency = Number(d.latency_mins) || 0;
    const awake = Number(d.awake_mins) || 0;
    const actualSleep = tib - latency - awake;
    const eff = tib > 0 ? (actualSleep / tib) * 100.0 : 0.0;
    
    totalDuration += actualSleep >= 0 ? actualSleep : 0;
    totalEfficiency += eff;
    totalLatency += latency;
    totalAwakenings += Number(d.awakenings) || 0;
    totalQuality += Number(d.quality) || 0;
    totalAlertness += Number(d.alertness) || 0;
    validDiaries++;
  });
  
  if (validDiaries === 0) {
    return {
      total_logs: diaries.length,
      average_duration_mins: 0,
      average_efficiency: 0.0,
      average_latency_mins: 0,
      average_awakenings: 0.0,
      average_quality: 0.0,
      average_alertness: 0.0
    };
  }
  
  return {
    total_logs: diaries.length,
    average_duration_mins: Math.round(totalDuration / validDiaries),
    average_efficiency: Math.round(totalEfficiency / validDiaries * 10) / 10,
    average_latency_mins: Math.round(totalLatency / validDiaries),
    average_awakenings: Math.round(totalAwakenings / validDiaries * 10) / 10,
    average_quality: Math.round(totalQuality / validDiaries * 10) / 10,
    average_alertness: Math.round(totalAlertness / validDiaries * 10) / 10
  };
}

// Tool definitions for DeepSeek API
const AGENT_TOOLS = [
  {
    type: "function",
    function: {
      name: "update_human_memory",
      description: "Overwrite/update the core human profile memory JSON. Use this when the user reveals personal profile details like age, sleep patterns, lifestyle, or goals.",
      parameters: {
        type: "object",
        properties: {
          new_content: {
            type: "string",
            description: "The updated JSON string representing the human profile core memory."
          }
        },
        required: ["new_content"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "update_persona_memory",
      description: "Update the advisor's internal persona guidelines or interaction rules in core memory.",
      parameters: {
        type: "object",
        properties: {
          new_content: {
            type: "string",
            description: "The updated persona memory text or JSON."
          }
        },
        required: ["new_content"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "insert_archival_memory",
      description: "Insert a new long-term fact or notable event into the archival memory database.",
      parameters: {
        type: "object",
        properties: {
          content: {
            type: "string",
            description: "A single clear fact or memory statement about the user."
          }
        },
        required: ["content"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "search_archival_memory",
      description: "Search the archival memory database for past facts or events about the user.",
      parameters: {
        type: "object",
        properties: {
          query: {
            type: "string",
            description: "The search keyword or topic to locate in long term memory."
          }
        },
        required: ["query"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "search_book",
      description: "Search Dr. Gregg Jacobs' book 'Say Good Night to Insomnia' for scientifically proven CBT-I methods, facts, guidelines, or quotes.",
      parameters: {
        type: "object",
        properties: {
          query: {
            type: "string",
            description: "The concept, chapter keyword, or query to search inside the book (e.g. '8-hour myth', 'sleep restriction', 'caffeine')."
          }
        },
        required: ["query"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "save_sleep_diary",
      description: "Log a completed 60-Second Sleep Diary entry for a specific date.",
      parameters: {
        type: "object",
        properties: {
          date: { type: "string", description: "The date of the sleep log in YYYY-MM-DD format (e.g., '2026-06-29')." },
          bed_time: { type: "string", description: "Time the user got into bed (HH:MM standard 24hr format, e.g. '23:15')." },
          light_out_time: { type: "string", description: "Time the user turned off the lights to sleep (HH:MM, e.g. '23:30')." },
          latency_mins: { type: "integer", description: "Minutes it took to fall asleep (integer)." },
          awakenings: { type: "integer", description: "Number of times the user woke up during the night (integer)." },
          awake_mins: { type: "integer", description: "Total minutes spent awake during the night after initial sleep (integer)." },
          wake_time: { type: "string", description: "Time of final awakening in the morning (HH:MM, e.g. '06:30')." },
          out_of_bed_time: { type: "string", description: "Time the user got out of bed (HH:MM, e.g. '06:45')." },
          quality: { type: "integer", description: "Subjective sleep quality score from 1 (poor) to 5 (excellent)." },
          alertness: { type: "integer", description: "Subjective daytime alertness score from 1 (fatigued) to 5 (refreshed)." },
          medications: { type: "string", description: "Name and dosage of any sleep medications taken, or 'None'." },
          notes: { type: "string", description: "Any additional comments or Negative Sleep Thoughts logged." }
        },
        required: ["date", "bed_time", "light_out_time", "latency_mins", "awakenings", "awake_mins", "wake_time", "out_of_bed_time", "quality", "alertness"]
      }
    }
  },
  {
    type: "function",
    function: {
      name: "get_sleep_stats",
      description: "Retrieve rolling averages and total count of sleep logs to calculate sleep efficiency.",
      parameters: {
        type: "object",
        properties: {}
      }
    }
  },
  {
    type: "function",
    function: {
      name: "update_cbt_week",
      description: "Advance or change the user's active CBT-I program week (e.g. -1 for interview, 0 for baseline, 1 for Week 1... 6 for Week 6).",
      parameters: {
        type: "object",
        properties: {
          week_num: {
            type: "integer",
            description: "The week index number (-1, 0, 1, 2, 3, 4, 5, 6)."
          }
        },
        required: ["week_num"]
      }
    }
  }
];

const SYSTEM_PROMPT_TEMPLATE = `You are a CBT-I Certified Sleep Advisor, based on Gregg Jacobs' book "Say Good Night to Insomnia". Your role is to act as a long-term sleep advisor, guiding the user through their cognitive behavioral therapy for insomnia.

The user is 53 years old. You will guide them through these phases:
1. **Initial Assessment (CBT Week -1)**: Conduct a comprehensive initial interview to establish their age, sleep history (latency, awakenings, duration), lifestyle (exercise, caffeine, alcohol), and goals. Use \`update_human_memory\` to save these details. Tell them they need to complete a daily 60-Second Sleep Diary for 7 days to establish their baseline.
2. **Baseline Week (CBT Week 0)**: The user is in their baseline logging phase. Ask them about their sleep, record diaries using the \`save_sleep_diary\` tool, and keep encouraging them. After they have logged 7 diaries, move them to Week 1.
3. **Week 1: Changing Thoughts (CBT Week 1)**: Teach cognitive restructuring. Challenge Negative Sleep Thoughts (NSTs) like the "8-hour sleep myth", explaining that 7 hours is average and healthier, and that sleep needs vary. Replace NSTs with Positive Sleep Thoughts (PSTs).
4. **Week 2: Sleep-Promoting Habits (CBT Week 2)**: Enforce sleep restriction (limiting time-in-bed to average baseline sleep duration, min 5 hours) and stimulus control rules (rising at same time daily, only bed when sleepy, get out of bed if awake 20+ mins, no naps).
5. **Week 3: Lifestyle & Environment (CBT Week 3)**: Review daily exercise (moderate, afternoon), light exposure (morning bright light), avoiding caffeine/alcohol, and sleep environment.
6. **Week 4: Relaxation Response (CBT Week 4)**: Introduce the Relaxation Response (abdominal breathing/breathing exercises, progressive muscle relaxation). Encourage them to use the app's Breathing Room.
7. **Week 5: Thinking Away Stress (CBT Week 5)**: Cognitive restructuring for daytime stress and negative self-talk.
8. **Week 6: Sleep-Enhancing Attitudes (CBT Week 6)**: Focus on optimism, commitment, control, challenge, and maintaining sleep habits long term.

=== LETTA CORE MEMORY ===
[CORE MEMORY - HUMAN PROFILE]
{human_mem}

[CORE MEMORY - ASSISTANT PERSONA]
{persona_mem}
=========================

=== RECALL MEMORY (RECENT HISTORY) ===
You have a memory of recent conversation below.

=== COGNITIVE INSTRUCTIONS ===
1. You are a Letta-style memory agent. You have the ability to read and write to your long-term memory.
2. When you learn new personal facts about the user (e.g. they sleep 6 hours, take Ambien, drink caffeine at 3 PM, run in mornings), immediately call \`update_human_memory\` to update the Human Profile JSON.
3. When you learn standalone long-term facts or specific events (e.g., "User started a new job on 2026-06-29"), save them to Archival Memory using \`insert_archival_memory\`.
4. If you need sleep science details, search Gregg Jacobs' book using the \`search_book\` tool.
5. If the user reports a night of sleep, use \`save_sleep_diary\` to log it. Make sure you get all details (bed time, light out time, latency, awakenings, awake mins, wake time, out of bed time, quality 1-5, alertness 1-5, medications). If some details are missing, ask for them or estimate them if user gives sufficient info.
6. Always ensure the user's current CBT program week is updated using \`update_cbt_week\` when they transition to next week.

=== VOICE CHAT STYLE ===
The user is speaking to you via a Voice Chat interface. Respond in a warm, caring, confident tone.
IMPORTANT: Keep your verbal responses CONCISE, clear, and easy to listen to (avoid long bullet lists or code blocks in voice mode unless asked, keep paragraphs to 1-3 short sentences).
`;

// Executes one turn of dialog with DeepSeek, handling tools locally
export async function runAgentTurn(userMessage, onStateChange = () => {}) {
  const apiKey = localStorage.getItem("deepseek_api_key");
  if (!apiKey) {
    return "Please enter your DeepSeek API key in the Settings tab to begin.";
  }

  initializeStorage();

  // Save user message to history
  const history = JSON.parse(localStorage.getItem("chat_history") || "[]");
  history.push({ sender: "user", message: userMessage, timestamp: new Date().toISOString() });
  localStorage.setItem("chat_history", JSON.stringify(history));

  // Prepare api messages
  const apiMessages = [];
  
  const humanMem = localStorage.getItem("core_memory_human") || "{}";
  const personaMem = localStorage.getItem("core_memory_persona") || "{}";
  
  const systemPrompt = SYSTEM_PROMPT_TEMPLATE
    .replace("{human_mem}", humanMem)
    .replace("{persona_mem}", personaMem);

  apiMessages.push({ role: "system", content: systemPrompt });

  // Map history (recent 12 messages)
  const recentHistory = history.slice(-12);
  // Include messages up to current turn
  recentHistory.forEach(h => {
    apiMessages.push({
      role: h.sender === "user" ? "user" : "assistant",
      content: h.message
    });
  });

  const url = "https://api.deepseek.com/chat/completions";
  const maxLoops = 5;
  let loopCount = 0;

  while (loopCount < maxLoops) {
    loopCount++;
    
    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `Bearer ${apiKey}`
        },
        body: JSON.stringify({
          model: "deepseek-chat",
          messages: apiMessages,
          tools: AGENT_TOOLS,
          tool_choice: "auto"
        })
      });

      if (!response.ok) {
        const errorText = await response.text();
        console.error(`DeepSeek API Error: ${response.status}`, errorText);
        return "I'm having trouble connecting to my sleep knowledge network. Please check your network and API key.";
      }

      const resJson = await response.json();
      const message = resJson.choices[0].message;

      // If there is no tool call, we are done
      if (!message.tool_calls || message.tool_calls.length === 0) {
        const aiText = message.content || "";
        
        // Save assistant message to history
        const finalHistory = JSON.parse(localStorage.getItem("chat_history") || "[]");
        finalHistory.push({ sender: "advisor", message: aiText, timestamp: new Date().toISOString() });
        localStorage.setItem("chat_history", JSON.stringify(finalHistory));
        
        onStateChange(); // Trigger UI state updates
        return aiText;
      }

      // Handle tool calls
      apiMessages.push(message);

      for (const toolCall of message.tool_calls) {
        const toolName = toolCall.function.name;
        const toolArgs = JSON.parse(toolCall.function.arguments);
        const toolId = toolCall.id || `call_${toolName}`;

        console.log(`Executing tool: ${toolName}`, toolArgs);
        let toolResult = "";

        try {
          if (toolName === "update_human_memory") {
            const content = toolArgs.new_content;
            localStorage.setItem("core_memory_human", content);
            toolResult = "Core Human Memory successfully updated.";
          } 
          else if (toolName === "update_persona_memory") {
            const content = toolArgs.new_content;
            localStorage.setItem("core_memory_persona", content);
            toolResult = "Core Persona Memory successfully updated.";
          } 
          else if (toolName === "insert_archival_memory") {
            const content = toolArgs.content;
            const archival = JSON.parse(localStorage.getItem("archival_memories") || "[]");
            archival.push({ content, created_at: new Date().toISOString() });
            localStorage.setItem("archival_memories", JSON.stringify(archival));
            toolResult = `Fact committed to Archival Memory: '${content}'`;
          } 
          else if (toolName === "search_archival_memory") {
            const query = toolArgs.query.toLowerCase();
            const archival = JSON.parse(localStorage.getItem("archival_memories") || "[]");
            const matches = archival.filter(m => m.content.toLowerCase().includes(query));
            toolResult = matches.length > 0 
              ? matches.map(m => `- [${m.created_at}]: ${m.content}`).join("\n")
              : "No matches found in long term memories.";
          } 
          else if (toolName === "search_book") {
            const matches = searchBook(toolArgs.query);
            toolResult = matches.length > 0 ? JSON.stringify(matches) : "No matches found in the book.";
          } 
          else if (toolName === "save_sleep_diary") {
            const diaries = JSON.parse(localStorage.getItem("sleep_diaries") || "[]");
            const index = diaries.findIndex(d => d.date === toolArgs.date);
            if (index !== -1) {
              diaries[index] = toolArgs;
            } else {
              diaries.push(toolArgs);
            }
            localStorage.setItem("sleep_diaries", JSON.stringify(diaries));
            toolResult = "Sleep diary logged successfully.";
          } 
          else if (toolName === "get_sleep_stats") {
            const stats = calculateSleepStats();
            toolResult = JSON.stringify(stats);
          } 
          else if (toolName === "update_cbt_week") {
            localStorage.setItem("cbt_week", toolArgs.week_num.toString());
            // Also update core human progress
            const human = JSON.parse(localStorage.getItem("core_memory_human") || "{}");
            if (human.cbt_progress) {
              human.cbt_progress.current_week = toolArgs.week_num;
              const descriptions = {
                "-1": "Initial Assessment Interview",
                "0": "Baseline Logging Week",
                "1": "Week 1: Changing Thoughts",
                "2": "Week 2: Sleep Habits",
                "3": "Week 3: Lifestyle & Environment",
                "4": "Week 4: Relaxation Response",
                "5": "Week 5: Thinking Away Stress",
                "6": "Week 6: Sleep Attitudes"
              };
              human.cbt_progress.current_week_description = descriptions[toolArgs.week_num.toString()] || "Maintenance";
              localStorage.setItem("core_memory_human", JSON.stringify(human, null, 2));
            }
            toolResult = `CBT Program week updated to ${toolArgs.week_num}.`;
          } 
          else {
            toolResult = `Error: Tool '${toolName}' not found.`;
          }
        } catch (err) {
          toolResult = `Error executing tool: ${err.message}`;
        }

        console.log(`Tool Result for ${toolName}:`, toolResult);
        
        apiMessages.push({
          role: "tool",
          tool_call_id: toolId,
          name: toolName,
          content: toolResult
        });
      }
      
      onStateChange(); // Sync components during execution

    } catch (e) {
      console.error(e);
      return "My processing encountered an issue while communicating. Let's try again in a moment.";
    }
  }

  return "I processed your request, but reached my tool-loop limit. How else can I support you tonight?";
}
