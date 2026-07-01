import React, { useState, useEffect } from 'react';
import { Eye, PlusCircle, Trash2, Brain, Database, User } from 'lucide-react';

export default function MemoryInspector({ onUpdate }) {
  const [humanMemory, setHumanMemory] = useState('{}');
  const [personaMemory, setPersonaMemory] = useState('{}');
  const [archivalMemories, setArchivalMemories] = useState([]);
  const [newFact, setNewFact] = useState('');

  useEffect(() => {
    loadMemory();
  }, []);

  const loadMemory = () => {
    setHumanMemory(localStorage.getItem('core_memory_human') || '{}');
    setPersonaMemory(localStorage.getItem('core_memory_persona') || '{}');
    setArchivalMemories(JSON.parse(localStorage.getItem('archival_memories') || '[]'));
  };

  const handleAddFact = (e) => {
    e.preventDefault();
    if (!newFact.trim()) return;

    const archival = JSON.parse(localStorage.getItem('archival_memories') || '[]');
    archival.unshift({
      content: newFact.trim(),
      created_at: new Date().toISOString()
    });
    localStorage.setItem('archival_memories', JSON.stringify(archival));
    
    setNewFact('');
    loadMemory();
    onUpdate();
  };

  const handleDeleteFact = (index) => {
    if (confirm('Delete this archival memory fact?')) {
      const archival = JSON.parse(localStorage.getItem('archival_memories') || '[]');
      archival.splice(index, 1);
      localStorage.setItem('archival_memories', JSON.stringify(archival));
      loadMemory();
      onUpdate();
    }
  };

  const formatTimestamp = (isoStr) => {
    try {
      const date = new Date(isoStr);
      return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    } catch {
      return isoStr;
    }
  };

  return (
    <div className="memory-inspector-grid">
      
      {/* Introduction Card */}
      <div className="glass-panel glowing" style={{ display: 'flex', gap: '1rem', alignItems: 'center', background: 'rgba(138, 79, 255, 0.05)', borderColor: 'rgba(138, 79, 255, 0.2)' }}>
        <Brain size={24} style={{ color: 'var(--purple)', flexShrink: 0 }} />
        <div style={{ fontSize: '0.85rem', lineHeight: '1.4', color: '#ded3ff' }}>
          <strong>Letta Cognitive Architecture:</strong> The Sleep Advisor uses a Core Memory loop. As you speak to it, it dynamically updates its human profile (Core Memory) and writes permanent facts about your sleep habits, stress, or goals (Archival Memory) to maintain deep long-term context.
        </div>
      </div>

      {/* Core Memory Sections */}
      <div className="dashboard-grid">
        <div className="glass-panel" style={{ marginBottom: 0 }}>
          <h3 className="section-title" style={{ fontSize: '1.1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <User size={16} style={{ color: 'var(--gold)' }} />
            Core Memory - Human Profile
          </h3>
          <p className="section-subtitle" style={{ fontSize: '0.8rem', marginBottom: '0.75rem' }}>
            What the advisor currently remembers about your sleep status.
          </p>
          <div className="code-block-container" style={{ maxHeight: '280px' }}>
            <pre className="code-block-content">{humanMemory}</pre>
          </div>
        </div>

        <div className="glass-panel" style={{ marginBottom: 0 }}>
          <h3 className="section-title" style={{ fontSize: '1.1rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Brain size={16} style={{ color: 'var(--purple)' }} />
            Core Memory - Persona Rules
          </h3>
          <p className="section-subtitle" style={{ fontSize: '0.8rem', marginBottom: '0.75rem' }}>
            The advisor's current behavioral rules and parameters.
          </p>
          <div className="code-block-container" style={{ maxHeight: '280px' }}>
            <pre className="code-block-content">{personaMemory}</pre>
          </div>
        </div>
      </div>

      {/* Archival Memory Section */}
      <div className="glass-panel">
        <h3 className="section-title" style={{ fontSize: '1.2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Database size={18} style={{ color: 'var(--cyan)' }} />
          Archival Memory Database
        </h3>
        <p className="section-subtitle">A semantic storage log of permanent facts and events compiled over time.</p>

        {/* Form to insert fact manually */}
        <form onSubmit={handleAddFact} style={{ display: 'flex', gap: '0.5rem', marginBottom: '1.5rem' }}>
          <input
            type="text"
            className="form-input"
            style={{ flex: 1 }}
            placeholder="Add a new permanent fact about yourself..."
            value={newFact}
            onChange={(e) => setNewFact(e.target.value)}
            required
          />
          <button type="submit" className="primary-btn">
            <PlusCircle size={18} />
            Insert Fact
          </button>
        </form>

        {/* Fact list display */}
        {archivalMemories.length === 0 ? (
          <p style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem', padding: '2rem 0' }}>
            No archival facts recorded yet. The advisor will insert them automatically during conversation.
          </p>
        ) : (
          <div className="fact-list">
            {archivalMemories.map((fact, index) => (
              <div key={index} className="fact-item">
                <div>
                  <span style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'block', marginBottom: '0.2rem' }}>
                    {formatTimestamp(fact.created_at)}
                  </span>
                  <span style={{ color: 'var(--text-primary)' }}>{fact.content}</span>
                </div>
                <button 
                  className="delete-btn" 
                  onClick={() => handleDeleteFact(index)}
                  title="Delete Fact"
                >
                  <Trash2 size={16} />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

    </div>
  );
}
