import React from "react";
import { useProjects } from "../../../../hooks/useProjects";

export default function ProjectPage() {
  const { projects } = useProjects();
  const project = projects[0]; // Prend le premier projet (juste un exemple)
  if (!project) return <div>Aucun projet disponible.</div>;
  return (
    <div>
      <h1>{project.name}</h1>
      <p>{project.description}</p>
    </div>
  );
}
