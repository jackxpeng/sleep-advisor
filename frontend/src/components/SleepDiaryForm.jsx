import React, { useState, useEffect } from 'react';
import { BookOpen, Table, PlusCircle, Sparkles } from 'lucide-react';

export default function SleepDiaryForm({ onUpdate }) {
  const todayStr = new Date().toISOString().split('T')[0];
  
  const [date, setDate] = useState(todayStr);
  const [bedTime, setBedTime] = useState('23:00');
  const [lightOutTime, setLightOutTime] = useState('23:15');
  const [latencyMins, setLatencyMins] = useState(30);
  const [awakenings, setAwakenings] = useState(1);
  const [awakeMins, setAwakeMins] = useState(15);
  const [wakeTime, setWakeTime] = useState('06:30');
  const [outOfBedTime, setOutOfBedTime] = useState('06:45');
  const [quality, setQuality] = useState(3);
  const [alertness, setAlertness] = useState(3);
  const [medications, setMedications] = useState('None');
  const [notes, setNotes] = useState('');
  
  const [diaries, setDiaries] = useState([]);
  
  // Real-time calculations
  const [calcStats, setCalcStats] = useState({ tib: 0, actual: 0, efficiency: 0.0 });

  useEffect(() => {
    loadDiaries();
  }, []);

  useEffect(() => {
    calculateLiveStats();
  }, [bedTime, lightOutTime, latencyMins, awakeMins, wakeTime, outOfBedTime]);

  const loadDiaries = () => {
    const logs = JSON.parse(localStorage.getItem('sleep_diaries') || '[]');
    // Sort logs descending by date
    logs.sort((a, b) => b.date.localeCompare(a.date));
    setDiaries(logs);
  };

  const calculateLiveStats = () => {
    const timeDiffMins = (t1, t2) => {
      try {
        const [h1, m1] = t1.split(':').map(Number);
        const [h2, m2] = t2.split(':').map(Number);
        let mins = (h2 * 60 + m2) - (h1 * 60 + m1);
        if (mins < 0) mins += 24 * 60; // crossed midnight
        return mins;
      } catch {
        return 0;
      }
    };

    const tib = timeDiffMins(lightOutTime || bedTime, outOfBedTime);
    const latency = Number(latencyMins) || 0;
    const awake = Number(awakeMins) || 0;
    const actual = Math.max(0, tib - latency - awake);
    const efficiency = tib > 0 ? Math.round((actual / tib) * 100 * 10) / 10 : 0;
    
    setCalcStats({ tib, actual, efficiency });
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    
    const newDiary = {
      date,
      bed_time: bedTime,
      light_out_time: lightOutTime,
      latency_mins: parseInt(latencyMins),
      awakenings: parseInt(awakenings),
      awake_mins: parseInt(awakeMins),
      wake_time: wakeTime,
      out_of_bed_time: outOfBedTime,
      quality: parseInt(quality),
      alertness: parseInt(alertness),
      medications,
      notes
    };

    const logs = JSON.parse(localStorage.getItem('sleep_diaries') || '[]');
    const index = logs.findIndex(d => d.date === date);
    
    if (index !== -1) {
      logs[index] = newDiary;
    } else {
      logs.push(newDiary);
    }
    
    localStorage.setItem('sleep_diaries', JSON.stringify(logs));
    alert('Sleep diary saved successfully.');
    
    // Clear inputs for next log
    setNotes('');
    loadDiaries();
    onUpdate();
  };

  const formatMins = (mins) => {
    const hrs = Math.floor(mins / 60);
    const remainder = mins % 60;
    return remainder > 0 ? `${hrs}h ${remainder}m` : `${hrs}h`;
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      
      {/* Real-time Sleep calculation feedback card */}
      <div className="glass-panel glowing" style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-around', alignItems: 'center', background: 'rgba(6, 214, 160, 0.03)', borderColor: 'rgba(6, 214, 160, 0.15)' }}>
        <div style={{ textAlign: 'center' }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Time In Bed</span>
          <div style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', fontWeight: 800, color: 'var(--text-primary)' }}>
            {formatMins(calcStats.tib)}
          </div>
        </div>
        
        <div style={{ textAlign: 'center' }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Actual Sleep Time</span>
          <div style={{ fontFamily: 'var(--font-title)', fontSize: '1.5rem', fontWeight: 800, color: 'var(--purple)' }}>
            {formatMins(calcStats.actual)}
          </div>
        </div>

        <div style={{ textAlign: 'center' }}>
          <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Sleep Efficiency</span>
          <div style={{ fontFamily: 'var(--font-title)', fontSize: '1.8rem', fontWeight: 800, color: calcStats.efficiency >= 85 ? 'var(--cyan)' : 'var(--gold)' }}>
            {calcStats.efficiency}%
          </div>
        </div>
      </div>

      <div className="glass-panel">
        <h2 className="section-title">60-Second Sleep Diary</h2>
        <p className="section-subtitle">Complete this diary every morning to track your CBT-I progress.</p>
        
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div className="dashboard-grid">
            <div className="form-group">
              <label className="form-label">Diary Date</label>
              <input type="date" className="form-input" value={date} onChange={(e) => setDate(e.target.value)} required />
            </div>
            
            <div className="form-group">
              <label className="form-label">Medications Taken</label>
              <input type="text" className="form-input" placeholder="e.g. None or Ambien 5mg" value={medications} onChange={(e) => setMedications(e.target.value)} />
            </div>
          </div>

          <div className="dashboard-grid">
            <div className="form-group">
              <label className="form-label">Time to Bed</label>
              <input type="time" className="form-input" value={bedTime} onChange={(e) => setBedTime(e.target.value)} required />
            </div>
            
            <div className="form-group">
              <label className="form-label">Lights Out Time</label>
              <input type="time" className="form-input" value={lightOutTime} onChange={(e) => setLightOutTime(e.target.value)} required />
            </div>
          </div>

          <div className="dashboard-grid">
            <div className="form-group">
              <label className="form-label">Time to Fall Asleep (Mins)</label>
              <input type="number" min="0" className="form-input" value={latencyMins} onChange={(e) => setLatencyMins(e.target.value)} required />
            </div>
            
            <div className="form-group">
              <label className="form-label">Number of Awakenings</label>
              <input type="number" min="0" className="form-input" value={awakenings} onChange={(e) => setAwakenings(e.target.value)} required />
            </div>
          </div>

          <div className="dashboard-grid">
            <div className="form-group">
              <label className="form-label">Total Time Awake in Night (Mins)</label>
              <input type="number" min="0" className="form-input" value={awakeMins} onChange={(e) => setAwakeMins(e.target.value)} required />
            </div>
            
            <div className="form-group">
              <label className="form-label">Final Wake Time</label>
              <input type="time" className="form-input" value={wakeTime} onChange={(e) => setWakeTime(e.target.value)} required />
            </div>
          </div>

          <div className="dashboard-grid">
            <div className="form-group">
              <label className="form-label">Time Out of Bed</label>
              <input type="time" className="form-input" value={outOfBedTime} onChange={(e) => setOutOfBedTime(e.target.value)} required />
            </div>
            
            <div className="form-group">
              <label className="form-label">Sleep Quality (1 - 5)</label>
              <select className="form-select" value={quality} onChange={(e) => setQuality(e.target.value)}>
                <option value="1">1 - Very Poor</option>
                <option value="2">2 - Poor</option>
                <option value="3">3 - Fair</option>
                <option value="4">4 - Good</option>
                <option value="5">5 - Excellent</option>
              </select>
            </div>
          </div>

          <div className="dashboard-grid">
            <div className="form-group">
              <label className="form-label">Daytime Alertness (1 - 5)</label>
              <select className="form-select" value={alertness} onChange={(e) => setAlertness(e.target.value)}>
                <option value="1">1 - Very Groggy / Fatigued</option>
                <option value="2">2 - Tired</option>
                <option value="3">3 - Fairly Alert</option>
                <option value="4">4 - Alert / Energetic</option>
                <option value="5">5 - Fully Refreshed</option>
              </select>
            </div>
            
            <div className="form-group">
              <label className="form-label">Negative Sleep Thoughts (NSTs) / Notes</label>
              <textarea className="form-textarea" rows="2" placeholder="e.g. Thought: If I don't sleep, I will fail my presentation." value={notes} onChange={(e) => setNotes(e.target.value)} />
            </div>
          </div>

          <button type="submit" className="primary-btn" style={{ alignSelf: 'flex-start' }}>
            <PlusCircle size={18} />
            Save Sleep Diary
          </button>
        </form>
      </div>

      {/* Diary Records Table */}
      <div className="glass-panel">
        <h3 className="section-title" style={{ fontSize: '1.25rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
          <Table size={18} style={{ color: 'var(--purple)' }} />
          Past Sleep Records
        </h3>
        <p className="section-subtitle">Your historical logs saved on this browser.</p>
        
        {diaries.length === 0 ? (
          <p style={{ textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.85rem', padding: '2rem 0' }}>
            No logs recorded yet. Create your first log above.
          </p>
        ) : (
          <div className="diary-table-container">
            <table className="diary-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Bed Window</th>
                  <th>Latency</th>
                  <th>Awak.</th>
                  <th>Awake Mins</th>
                  <th>Efficiency</th>
                  <th>Quality</th>
                </tr>
              </thead>
              <tbody>
                {diaries.map((d, index) => {
                  // Calculate row-specific sleep efficiency
                  const getRowEfficiency = () => {
                    const timeDiff = (t1, t2) => {
                      const [h1, m1] = t1.split(':').map(Number);
                      const [h2, m2] = t2.split(':').map(Number);
                      let mins = (h2 * 60 + m2) - (h1 * 60 + m1);
                      if (mins < 0) mins += 24 * 60;
                      return mins;
                    };
                    const tib = timeDiff(d.light_out_time || d.bed_time, d.out_of_bed_time);
                    const act = Math.max(0, tib - Number(d.latency_mins) - Number(d.awake_mins));
                    return tib > 0 ? Math.round((act / tib) * 100) : 0;
                  };
                  
                  const eff = getRowEfficiency();
                  
                  return (
                    <tr key={index}>
                      <td style={{ fontWeight: 600 }}>{d.date}</td>
                      <td>{d.light_out_time} - {d.out_of_bed_time}</td>
                      <td>{d.latency_mins}m</td>
                      <td>{d.awakenings}</td>
                      <td>{d.awake_mins}m</td>
                      <td style={{ color: eff >= 85 ? 'var(--cyan)' : 'var(--gold)', fontWeight: 600 }}>
                        {eff}%
                      </td>
                      <td>{d.quality}/5</td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
