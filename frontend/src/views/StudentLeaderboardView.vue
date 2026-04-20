<script setup lang="ts">

import SceneComponent from "@/components/SceneComponent.vue";
</script>

<template>
  <SceneComponent title="Ledertavle" previous="/student-home" background="/StartBackground.jpg">
    <div class="leaderboard-container">
      <div class="leaderboard-card">
        <h1>Klasse rangering</h1>
        <table class="student-leaderboard-table">
          <thead>
            <tr>
              <th>Plassering</th>
              <th>Klasse</th>
              <th>Poeng</th>
            </tr>
          </thead>
          <tbody>

          </tbody>

        </table>
      </div>
    </div>

  </SceneComponent>

</template>

<style scoped>
.leaderboard-container {
  display: flex;
  justify-content: center;
  padding: 2rem;
}

.leaderboard-card {
  width: 100%;
  max-width: 800px;
  background: rgba(255, 255, 255, 0.9);
  border-radius: 16px;
  padding: 2rem;
}

.leaderboard-card h1 {
  text-align: center;
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
}

.student-leaderboard-table {
  width: 100%;
  border-collapse: collapse;
}

.leaderboard-table th,
.leaderboard-table td {
  padding: 1rem;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

.leaderboard-table thead {
  background-color: #2c3e50;
  color: white;
}
</style>
